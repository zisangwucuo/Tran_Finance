package com.transfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.transfinance.entity.Company;
import com.transfinance.entity.CompanyUser;
import com.transfinance.mapper.CompanyMapper;
import com.transfinance.mapper.CompanyMissingDataMapper;
import com.transfinance.mapper.CompanyUserMapper;
import com.transfinance.service.CompanyBootstrapService;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.support.CompanyIdentityResolver;
import com.transfinance.support.IndustryDataCatalog.IndustryMeta;
import com.transfinance.util.CompanyCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Map;

/**
 * 从历史评估数据中初始化企业端基础账号。
 */
@Service
public class CompanyBootstrapServiceImpl implements CompanyBootstrapService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CompanyMapper companyMapper;
    private final CompanyUserMapper companyUserMapper;
    private final CompanyMissingDataMapper missingDataMapper;
    private final String defaultPassword;

    public CompanyBootstrapServiceImpl(CompanyMapper companyMapper,
                                       CompanyUserMapper companyUserMapper,
                                       CompanyMissingDataMapper missingDataMapper,
                                       @Value("${company.default-password:}") String defaultPassword) {
        this.companyMapper = companyMapper;
        this.companyUserMapper = companyUserMapper;
        this.missingDataMapper = missingDataMapper;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bootstrapCompaniesFromHistory() {
        bootstrapCompaniesFromHistory(defaultPassword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bootstrapCompaniesFromHistory(String defaultPassword) {
        for (IndustryMeta industry : IndustryDataCatalog.industries().values()) {
            bootstrapCompaniesFromTable(industry.tableName(), industry, defaultPassword);
            bootstrapCompaniesFromTable(industry.pendingTableName(), industry, defaultPassword);
        }
    }

    private void bootstrapCompaniesFromTable(String tableName, IndustryMeta industry, String defaultPassword) {
        for (Map<String, Object> row : missingDataMapper.selectDistinctCompanies(tableName)) {
            String companyName = asText(row.get("company_name"));
            if (!StringUtils.hasText(companyName)) {
                continue;
            }
            String stockCode = normalizeStockCode(asText(row.get("stock_code")));
            Company company = CompanyIdentityResolver.find(companyMapper, companyName, industry.key(), stockCode);
            if (company == null) {
                company = createCompany(companyName, asText(row.get("company_type")), stockCode, industry);
            }
            ensureDefaultUser(company, defaultPassword);
        }
    }

    private Company createCompany(String companyName, String companyType, String stockCode, IndustryMeta industry) {
        Company company = new Company();
        company.setCompanyCode(generateUniqueCompanyCode(companyName));
        company.setCompanyName(companyName);
        company.setCompanyType(normalizeCompanyType(companyType));
        company.setStockCode(stockCode);
        company.setIndustryKey(industry.key());
        company.setIndustryName(industry.name());
        companyMapper.insert(company);
        return company;
    }

    private void ensureDefaultUser(Company company, String defaultPassword) {
        if (!StringUtils.hasText(defaultPassword)) {
            return;
        }
        Long exists = companyUserMapper.selectCount(new LambdaQueryWrapper<CompanyUser>()
                .eq(CompanyUser::getCompanyCode, company.getCompanyCode()));
        if (exists != null && exists > 0) {
            return;
        }
        String username = uniqueUsername(company.getCompanyCode());
        CompanyUser user = new CompanyUser();
        user.setCompanyCode(company.getCompanyCode());
        user.setUsername(username);
        user.setPasswordHash(defaultPassword);
        companyUserMapper.insert(user);
    }

    private String uniqueUsername(String baseUsername) {
        if (companyUserMapper.selectCount(new LambdaQueryWrapper<CompanyUser>()
                .eq(CompanyUser::getUsername, baseUsername)) == 0) {
            return baseUsername;
        }
        for (int i = 0; i < 20; i++) {
            String username = baseUsername + "_" + SECURE_RANDOM.nextInt(10000);
            Long exists = companyUserMapper.selectCount(new LambdaQueryWrapper<CompanyUser>()
                    .eq(CompanyUser::getUsername, username));
            if (exists == null || exists == 0) {
                return username;
            }
        }
        return baseUsername + "_" + System.currentTimeMillis();
    }

    private String generateUniqueCompanyCode(String companyName) {
        String baseCode = CompanyCodeGenerator.initials(companyName);
        if (companyMapper.selectById(baseCode) == null) {
            return baseCode;
        }
        for (int i = 0; i < 20; i++) {
            String nextCode = baseCode + "_" + SECURE_RANDOM.nextInt(10000);
            if (companyMapper.selectById(nextCode) == null) {
                return nextCode;
            }
        }
        return baseCode + "_" + System.currentTimeMillis();
    }

    private String normalizeCompanyType(String value) {
        if ("上市公司".equals(value)) {
            return "上市企业";
        }
        if ("非上市公司".equals(value)) {
            return "非上市企业";
        }
        return StringUtils.hasText(value) ? value : "上市企业";
    }

    private String normalizeStockCode(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String asText(Object value) {
        return value == null ? null : value.toString().trim();
    }
}
