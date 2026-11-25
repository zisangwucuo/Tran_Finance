package com.transfinance.service;

import com.transfinance.mapper.MaterialExtractionMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** 可选本机MySQL联调：只建新增材料表并查询，不修改企业评估记录。 */
@EnabledIfSystemProperty(named="material.database",matches="true")
class MaterialMapperIntegrationTest {
    @Test void schemaAndOwnedTargetsExecuteOnMysql() {
        String host=System.getenv().getOrDefault("DB_HOST","127.0.0.1");
        String port=System.getenv().getOrDefault("DB_PORT","3306");
        String db=System.getenv().getOrDefault("DB_NAME","trans_finance");
        var dataSource=new DriverManagerDataSource("jdbc:mysql://"+host+":"+port+"/"+db+"?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                System.getenv().getOrDefault("DB_USERNAME","root"),System.getenv("DB_PASSWORD"));
        var config=new Configuration(new Environment("material-test",new JdbcTransactionFactory(),dataSource));
        config.addMapper(MaterialExtractionMapper.class);
        try(var session=new SqlSessionFactoryBuilder().build(config).openSession()) {
            var mapper=session.getMapper(MaterialExtractionMapper.class);
            mapper.createTasks(); mapper.createAudit();
            assertTrue(mapper.list("__material_test_nonexistent__").isEmpty());
            assertTrue(mapper.targets("steel_pending_company_data","__material_test_nonexistent__",2024).isEmpty());
            assertNull(mapper.lockTarget("steel_pending_company_data",1L,"__material_test_nonexistent__",2024));
            assertTrue(mapper.adoptedRows("__missing__","__material_test_nonexistent__").isEmpty());
            session.rollback();
        }
    }
}
