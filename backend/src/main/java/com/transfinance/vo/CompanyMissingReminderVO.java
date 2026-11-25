package com.transfinance.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 企业缺失数据提醒。
 */
public class CompanyMissingReminderVO {

    private Long id;
    private String companyCode;
    private String companyName;
    private String institutionCode;
    private String institutionName;
    private String industryKey;
    private String industryName;
    private String sourceTable;
    private Long sourceRowId;
    private Integer dataYear;
    @JsonProperty("missingColumnsJson")
    private String missingColumnsJson;
    @JsonProperty("changedColumnsJson")
    private String changedColumnsJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;

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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

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

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public Long getSourceRowId() {
        return sourceRowId;
    }

    public void setSourceRowId(Long sourceRowId) {
        this.sourceRowId = sourceRowId;
    }

    public Integer getDataYear() {
        return dataYear;
    }

    public void setDataYear(Integer dataYear) {
        this.dataYear = dataYear;
    }

    public String getMissingColumnsJson() {
        return missingColumnsJson;
    }

    public void setMissingColumnsJson(String missingColumnsJson) {
        this.missingColumnsJson = missingColumnsJson;
    }

    public String getChangedColumnsJson() {
        return changedColumnsJson;
    }

    public void setChangedColumnsJson(String changedColumnsJson) {
        this.changedColumnsJson = changedColumnsJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
