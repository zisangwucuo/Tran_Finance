package com.transfinance.service;

import com.transfinance.mapper.CustomerOverviewMapper;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.entity.FinancialInstitution;
import com.transfinance.controller.CustomerOverviewController;
import com.transfinance.exception.GlobalExceptionHandler;
import com.transfinance.service.impl.CustomerOverviewServiceImpl;
import com.transfinance.support.IndustryDataCatalog;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 可选真实 MySQL 只读验证，不启动自动初始化器，不创建表、不写入业务数据。 */
@EnabledIfSystemProperty(named = "overview.database", matches = "true")
class CustomerOverviewDatabaseTest {
    @Test void allIndustryTablesAndBankScopeExecuteOnMysql() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:mysql://"
                + System.getenv().getOrDefault("DB_HOST", "127.0.0.1") + ":"
                + System.getenv().getOrDefault("DB_PORT", "3306") + "/"
                + System.getenv().getOrDefault("DB_NAME", "trans_finance") + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                System.getenv().getOrDefault("DB_USERNAME", "root"), System.getenv("DB_PASSWORD"));
        var config = new Configuration(new Environment("overview-readonly", new JdbcTransactionFactory(), dataSource));
        config.addMapper(CustomerOverviewMapper.class);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            session.getConnection().setReadOnly(true);
            var mapper = session.getMapper(CustomerOverviewMapper.class);
            try (var statement = session.getConnection().createStatement();
                 var banks = statement.executeQuery("select institution_code, institution_name from financial_institution")) {
                while (banks.next()) {
                    long expectedRecords = 0;
                    for (var industry : IndustryDataCatalog.industries().values()) {
                        for (var table : new String[]{industry.tableName(), industry.pendingTableName()}) {
                            var rows = mapper.selectRecords(table, banks.getString(1), banks.getString(2));
                            expectedRecords += rows.size();
                            assertTrue(rows.stream().allMatch(row -> row.get("dataYear") instanceof Number));
                            // 独立计数SQL校验，避免错误的身份关联造成记录膨胀或减少。
                            try (var count = session.getConnection().prepareStatement("select count(*) from " + table
                                    + " t where t.institution_code = ? or (coalesce(t.institution_code, '') = ''"
                                    + " and t.institution_name = ? and (select count(*) from financial_institution where institution_name = ?) = 1)")) {
                                count.setString(1, banks.getString(1)); count.setString(2, banks.getString(2)); count.setString(3, banks.getString(2));
                                try (var expected = count.executeQuery()) { expected.next(); assertEquals(expected.getLong(1), rows.size()); }
                            }
                        }
                    }
                    assertNotNull(mapper.selectNoticeCounts(banks.getString(1), null));
                    assertNotNull(mapper.selectNoticeCounts(banks.getString(1), 2024));
                    // 实际数据库查询接入服务与MVC输出，校验字段序列化及全行记录总数。
                    var institution = new FinancialInstitution();
                    institution.setInstitutionCode(banks.getString(1));
                    institution.setInstitutionName(banks.getString(2));
                    var institutionMapper = mock(FinancialInstitutionMapper.class);
                    when(institutionMapper.selectById(institution.getInstitutionCode())).thenReturn(institution);
                    var service = new CustomerOverviewServiceImpl(mapper, institutionMapper);
                    var mvc = MockMvcBuilders.standaloneSetup(new CustomerOverviewController(service))
                            .setControllerAdvice(new GlobalExceptionHandler()).build();
                    var login = new MockHttpSession();
                    login.setAttribute("financialInstitutionId", institution.getInstitutionCode());
                    mvc.perform(get("/api/customer-overview").session(login))
                            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totals.records").value(expectedRecords))
                            .andExpect(jsonPath("$.data.industries.length()").value(8))
                            .andExpect(jsonPath("$.data.companyName").doesNotExist());
                }
            }
            for (var industry : IndustryDataCatalog.industries().values()) {
                assertTrue(mapper.selectRecords(industry.tableName(), "__unknown_bank__", "__unknown_bank__").isEmpty());
                assertTrue(mapper.selectRecords(industry.pendingTableName(), "__unknown_bank__", "__unknown_bank__").isEmpty());
            }
            session.rollback();
        }
    }
}
