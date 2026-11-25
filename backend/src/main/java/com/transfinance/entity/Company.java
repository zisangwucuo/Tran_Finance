package com.transfinance.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 企业基础信息实体。
 */
@TableName("company")
public class Company {

    /** 企业编码，由企业名称拼音首字母生成。 */
    @TableId
    private String companyCode;

    /** 企业名称。 */
    private String companyName;

    /** 企业类型：上市企业/非上市企业。 */
    private String companyType;

    /** 股票代码，非上市企业允许为空或短横线。 */
    private String stockCode;

    /** 所属行业键。 */
    private String industryKey;

    /** 所属行业名称。 */
    private String industryName;

    /** 联系人。 */
    private String contactName;

    /** 联系方式。 */
    private String contactPhone;

    /** 地址。 */
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

    public String getIndustryName() {
        return industryName;
    }

    public void setIndustryName(String industryName) {
        this.industryName = industryName;
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
