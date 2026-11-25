package com.transfinance.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 金融机构基础信息实体。
 */
@TableName("financial_institution")
public class FinancialInstitution {

    /** 金融机构编码。 */
    @TableId
    private String institutionCode;

    /** 金融机构名称。 */
    private String institutionName;

    /** 机构类型：bank/guarantee/other。 */
    private String institutionType;

    /** 联系人。 */
    private String contactName;

    /** 联系电话。 */
    private String contactPhone;

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

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
}
