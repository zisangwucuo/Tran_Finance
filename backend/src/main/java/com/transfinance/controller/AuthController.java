package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.LoginRequest;
import com.transfinance.dto.RegisterRequest;
import com.transfinance.service.AuthService;
import com.transfinance.vo.LoginResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 金融机构认证接口。
 */
@RestController
@RequestMapping("/api/financial-auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        LoginResponse user = authService.login(request);
        bindSession(http, user);
        return ApiResponse.success(user);
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        LoginResponse user = authService.register(request);
        bindSession(http, user);
        return ApiResponse.success(user);
    }

    /** PSI身份以服务端登录会话为准，不能信任浏览器传来的机构编码。 */
    private void bindSession(HttpServletRequest request, LoginResponse user) {
        var oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        var session = request.getSession(true);
        session.setAttribute("financialInstitutionId", user.getInstitutionId());
        session.setMaxInactiveInterval(1800);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return ApiResponse.success(null);
    }
}
