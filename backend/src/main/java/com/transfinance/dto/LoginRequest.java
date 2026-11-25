package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 金融机构用户登录请求。
 */
public class LoginRequest {

    /** 登录用户名。 */
    @NotBlank(message = "不能为空")
    private String username;

    /** 登录密码，仅用于本次校验，不落库、不打印日志。 */
    @NotBlank(message = "不能为空")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
