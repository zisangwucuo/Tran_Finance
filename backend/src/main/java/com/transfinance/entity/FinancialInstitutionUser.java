package com.transfinance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 金融机构登录用户实体。
 */
@TableName("financial_institution_user")
public class FinancialInstitutionUser {

    /** 金融机构用户ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属金融机构编码。 */
    private String institutionId;

    /** 用户名。 */
    private String username;

    /** 演示版登录密码字段，当前按用户要求保存明文密码。 */
    private String passwordHash;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
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
