package com.transfinance.dto;

/**
 * 企业基础账号补齐请求。
 */
public class CompanyBootstrapRequest {

    /** 本次补齐企业用户时使用的默认登录密码，不写入配置文件或日志。 */
    private String defaultPassword;

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }
}
