package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 企业基础信息修改请求。企业编码和股票代码不允许修改。
 */
public class CompanyProfileUpdateRequest {

    @NotBlank(message = "不能为空")
    private String companyCode;

    @NotBlank(message = "不能为空")
    @Size(max = 200, message = "长度不能超过200个字符")
    private String companyName;

    @NotBlank(message = "不能为空")
    @Size(max = 30, message = "长度不能超过30个字符")
    private String companyType;

    @NotBlank(message = "不能为空")
    private String industryKey;

    @Size(max = 100, message = "长度不能超过100个字符")
    private String contactName;

    @Size(max = 30, message = "长度不能超过30个字符")
    private String contactPhone;

    @Size(max = 500, message = "长度不能超过500个字符")
    private String address;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

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
}
