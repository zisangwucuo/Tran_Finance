package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 金融机构注册请求。
 */
public class RegisterRequest {

    /** 金融机构名称。 */
    @NotBlank(message = "不能为空")
    @Size(max = 200, message = "长度不能超过200个字符")
    private String institutionName;

    /** 机构类型：bank/guarantee/other。 */
    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过64个字符")
    private String institutionType;

    /** 联系人。 */
    @Size(max = 100, message = "长度不能超过100个字符")
    private String contactName;

    /** 联系电话。 */
    @Size(max = 30, message = "长度不能超过30个字符")
    private String contactPhone;

    /** 登录用户名。 */
    @NotBlank(message = "不能为空")
    @Size(min = 3, max = 64, message = "长度必须在3到64个字符之间")
    private String username;

    /** 登录密码，当前演示版直接保存到数据库密码字段。 */
    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 64, message = "长度必须在6到64个字符之间")
    private String password;

    /** 确认密码。 */
    @NotBlank(message = "不能为空")
    private String confirmPassword;

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(String institutionType) {
        this.institutionType = institutionType;
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
