package com.transfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.transfinance.dto.LoginRequest;
import com.transfinance.dto.RegisterRequest;
import com.transfinance.entity.FinancialInstitution;
import com.transfinance.entity.FinancialInstitutionUser;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.mapper.FinancialInstitutionUserMapper;
import com.transfinance.service.AuthService;
import com.transfinance.vo.LoginResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 金融机构认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int MAX_CODE_GENERATE_RETRY = 20;

    private final FinancialInstitutionUserMapper userMapper;
    private final FinancialInstitutionMapper institutionMapper;

    public AuthServiceImpl(FinancialInstitutionUserMapper userMapper,
                           FinancialInstitutionMapper institutionMapper) {
        this.userMapper = userMapper;
        this.institutionMapper = institutionMapper;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        FinancialInstitutionUser user = userMapper.selectOne(new LambdaQueryWrapper<FinancialInstitutionUser>()
                .eq(FinancialInstitutionUser::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordMatches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setInstitutionId(user.getInstitutionId());
        response.setUsername(user.getUsername());
        FinancialInstitution institution = institutionMapper.selectById(user.getInstitutionId());
        response.setInstitutionName(institution == null ? null : institution.getInstitutionName());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<FinancialInstitutionUser>()
                .eq(FinancialInstitutionUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }

        String normalizedInstitutionType = normalizeInstitutionType(request.getInstitutionType());
        String institutionCode = generateUniqueInstitutionCode(normalizedInstitutionType);
        FinancialInstitution institution = new FinancialInstitution();
        institution.setInstitutionCode(institutionCode);
        institution.setInstitutionName(request.getInstitutionName());
        institution.setInstitutionType(normalizedInstitutionType);
        institution.setContactName(request.getContactName());
        institution.setContactPhone(request.getContactPhone());
        institutionMapper.insert(institution);

        FinancialInstitutionUser user = new FinancialInstitutionUser();
        user.setInstitutionId(institutionCode);
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        userMapper.insert(user);

        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setInstitutionId(institutionCode);
        response.setUsername(user.getUsername());
        response.setInstitutionName(institution.getInstitutionName());
        return response;
    }

    /**
     * 将机构类型统一成可用于机构编码前缀的小写字母数字下划线形式。
     */
    private String normalizeInstitutionType(String institutionType) {
        String normalized = institutionType.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("机构类型格式不正确");
        }
        return normalized;
    }

    /**
     * 根据机构类型生成不重复的金融机构编码，格式为“机构类型_数字或小写字母”。
     */
    private String generateUniqueInstitutionCode(String institutionType) {
        for (int i = 0; i < MAX_CODE_GENERATE_RETRY; i++) {
            String institutionCode = institutionType + "_" + randomCodeSuffix(4);
            if (institutionMapper.selectById(institutionCode) == null) {
                return institutionCode;
            }
        }
        throw new BusinessException("机构编码生成失败，请稍后重试");
    }

    /**
     * 生成由数字和小写字母组成的随机后缀。
     */
    private String randomCodeSuffix(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CODE_CHARS[SECURE_RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return builder.toString();
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        return storedPassword.equals(rawPassword);
    }
}
