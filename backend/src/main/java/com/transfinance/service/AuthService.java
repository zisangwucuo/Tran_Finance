package com.transfinance.service;

import com.transfinance.dto.LoginRequest;
import com.transfinance.dto.RegisterRequest;
import com.transfinance.vo.LoginResponse;

/**
 * 金融机构认证服务。
 */
public interface AuthService {

    /**
     * 登录校验。
     *
     * @param request 登录请求
     * @return 登录成功后的非敏感用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 注册新的金融机构用户。
     *
     * @param request 注册请求
     * @return 注册成功后的非敏感用户信息
     */
    LoginResponse register(RegisterRequest request);
}
