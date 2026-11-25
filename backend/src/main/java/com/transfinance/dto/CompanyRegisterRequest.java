package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 企业端注册请求。
 */
public class CompanyRegisterRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 200, message = "长度不能超过200个字符")
    private String companyName;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过30个字符")
    private String companyType;

    @Size(max = 64, message = "长度不能超过64个字符")
    private String stockCode;

    @NotBlank(message = "不能为空")
    private String industryKey;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String contactName;

    @Size(max = 30, message = "长度不能超过30个字符")
    private String contactPhone;

    @Size(max = 500, message = "长度不能超过500个字符")
    private String address;

    @NotBlank(message = "不能为空")
    @Size(min = 3, max = 64, message = "长度必须在3到64个字符之间")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 64, message = "长度必须在6到64个字符之间")
    private String password;

    @NotBlank(message = "不能为空")
    private String confirmPassword;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getIndustryKey() {
        return industryKey;
    }

    public void setIndustryKey(String industryKey) {
        this.industryKey = industryKey;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
