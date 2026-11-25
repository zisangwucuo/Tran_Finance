package com.transfinance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 企业端登录用户实体。
 */
@TableName("company_user")
public class CompanyUser {

    /** 企业用户ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 绑定企业编码。 */
    private String companyCode;

    /** 企业端登录用户名。 */
    private String username;

    /** 演示版登录密码字段，当前按用户要求保存明文密码。 */
    private String passwordHash;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
