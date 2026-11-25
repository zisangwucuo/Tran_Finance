package com.transfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.transfinance.dto.CompanyProfileUpdateRequest;
import com.transfinance.dto.CompanyRegisterRequest;
import com.transfinance.dto.LoginRequest;
import com.transfinance.entity.Company;
import com.transfinance.entity.CompanyUser;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.CompanyMapper;
import com.transfinance.mapper.CompanyUserMapper;
import com.transfinance.service.CompanyAuthService;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.support.IndustryDataCatalog.IndustryMeta;
import com.transfinance.util.CompanyCodeGenerator;
import com.transfinance.vo.CompanyLoginResponse;
import com.transfinance.vo.CompanyProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

/**
 * 企业端认证与资料服务实现。
 */
@Service
public class CompanyAuthServiceImpl implements CompanyAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CompanyMapper companyMapper;
    private final CompanyUserMapper companyUserMapper;

    public CompanyAuthServiceImpl(CompanyMapper companyMapper,
                                  CompanyUserMapper companyUserMapper) {
        this.companyMapper = companyMapper;
        this.companyUserMapper = companyUserMapper;
    }

    @Override
    public CompanyLoginResponse login(LoginRequest request) {
        CompanyUser user = companyUserMapper.selectOne(new LambdaQueryWrapper<CompanyUser>()
                .eq(CompanyUser::getUsername, request.getUsername()));
        if (user == null || !request.getPassword().equals(user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        Company company = requireCompany(user.getCompanyCode());
        return toLoginResponse(user, company);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyLoginResponse register(CompanyRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        validateCompanyType(request.getCompanyType(), request.getStockCode());
        Long exists = companyUserMapper.selectCount(new LambdaQueryWrapper<CompanyUser>()
                .eq(CompanyUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        IndustryMeta industry = IndustryDataCatalog.requireIndustry(request.getIndustryKey());
        // 注册不能借由股票代码缺失再建同名企业，也不能直接认领已有企业账号。
        Long companyExists = companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getCompanyName, request.getCompanyName().trim())
                .eq(Company::getIndustryKey, industry.key()));
        if (companyExists != null && companyExists > 0) {
            throw new BusinessException("该企业已有资料，请使用已有企业账号登录或联系管理员核实身份");
        }
        String companyCode = generateUniqueCompanyCode(request.getCompanyName());

        Company company = new Company();
        company.setCompanyCode(companyCode);
        company.setCompanyName(request.getCompanyName());
        company.setCompanyType(request.getCompanyType());
        company.setStockCode(normalizeStockCode(request.getCompanyType(), request.getStockCode()));
        company.setIndustryKey(industry.key());
        company.setIndustryName(industry.name());
        company.setContactName(request.getContactName());
        company.setContactPhone(request.getContactPhone());
        company.setAddress(request.getAddress());
        companyMapper.insert(company);

        CompanyUser user = new CompanyUser();
        user.setCompanyCode(companyCode);
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        companyUserMapper.insert(user);
        return toLoginResponse(user, company);
    }

    @Override
    public CompanyProfileVO profile(String companyCode) {
        return toProfileVO(requireCompany(companyCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyProfileVO updateProfile(CompanyProfileUpdateRequest request) {
        Company company = requireCompany(request.getCompanyCode());
        String oldName = company.getCompanyName();
        IndustryMeta industry = IndustryDataCatalog.requireIndustry(request.getIndustryKey());
        validateCompanyType(request.getCompanyType(), company.getStockCode());

        company.setCompanyName(request.getCompanyName());
        company.setCompanyType(request.getCompanyType());
        company.setIndustryKey(industry.key());
        company.setIndustryName(industry.name());
        company.setContactName(request.getContactName());
        company.setContactPhone(request.getContactPhone());
        company.setAddress(request.getAddress());
        companyMapper.updateById(company);

        for (IndustryMeta item : IndustryDataCatalog.industries().values()) {
            companyMapper.syncCompanyProfileToDataTable(item.tableName(), oldName, company.getStockCode(), company.getCompanyName(), company.getCompanyType());
            companyMapper.syncCompanyProfileToDataTable(item.pendingTableName(), oldName, company.getStockCode(), company.getCompanyName(), company.getCompanyType());
        }
        return toProfileVO(company);
    }

    private Company requireCompany(String companyCode) {
        Company company = companyMapper.selectById(companyCode);
        if (company == null) {
            throw new BusinessException("企业不存在，请重新登录");
        }
        return company;
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
        throw new BusinessException("企业编码生成失败，请稍后重试");
    }

    private void validateCompanyType(String companyType, String stockCode) {
        if (!"上市企业".equals(companyType) && !"非上市企业".equals(companyType)) {
            throw new BusinessException("企业类型只能选择上市企业或非上市企业");
        }
        if ("上市企业".equals(companyType) && !StringUtils.hasText(stockCode)) {
            throw new BusinessException("上市企业必须填写股票代码");
        }
    }

    private String normalizeStockCode(String companyType, String stockCode) {
        if ("非上市企业".equals(companyType)) {
            return "-";
        }
        return stockCode == null ? null : stockCode.trim();
    }

    private CompanyLoginResponse toLoginResponse(CompanyUser user, Company company) {
        CompanyLoginResponse response = new CompanyLoginResponse();
        response.setId(user.getId());
        response.setCompanyCode(company.getCompanyCode());
        response.setCompanyName(company.getCompanyName());
        response.setUsername(user.getUsername());
        response.setIndustryKey(company.getIndustryKey());
        response.setIndustryName(company.getIndustryName());
        return response;
    }

    private CompanyProfileVO toProfileVO(Company company) {
        CompanyProfileVO response = new CompanyProfileVO();
        response.setCompanyCode(company.getCompanyCode());
        response.setCompanyName(company.getCompanyName());
        response.setCompanyType(company.getCompanyType());
        response.setStockCode(company.getStockCode());
        response.setIndustryKey(company.getIndustryKey());
        response.setIndustryName(company.getIndustryName());
        response.setContactName(company.getContactName());
        response.setContactPhone(company.getContactPhone());
        response.setAddress(company.getAddress());
        return response;
    }
}
