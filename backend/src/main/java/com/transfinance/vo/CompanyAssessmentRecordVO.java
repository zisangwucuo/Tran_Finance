package com.transfinance.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企业端历史数据或待评估数据列表记录。
 */
public class CompanyAssessmentRecordVO {

    private Long id;
    private String industryKey;
    private String industryName;
    private String institutionCode;
    private String institutionName;
    private String companyCode;
    private String companyName;
    private String stockCode;
    private String companyType;
    private Integer dataYear;
    private BigDecimal transitionScore;
    private Integer verifiedTransitionPerformance;
    private String assessmentStatus;
    private String resultLabel;
    private String assessmentAdvice;
    private List<String> suggestions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public Integer getDataYear() {
        return dataYear;
    }

    public void setDataYear(Integer dataYear) {
        this.dataYear = dataYear;
    }

    public BigDecimal getTransitionScore() {
        return transitionScore;
    }

    public void setTransitionScore(BigDecimal transitionScore) {
        this.transitionScore = transitionScore;
    }

    public Integer getVerifiedTransitionPerformance() {
        return verifiedTransitionPerformance;
    }

    public void setVerifiedTransitionPerformance(Integer verifiedTransitionPerformance) {
        this.verifiedTransitionPerformance = verifiedTransitionPerformance;
    }

    public String getAssessmentStatus() {
        return assessmentStatus;
    }

    public void setAssessmentStatus(String assessmentStatus) {
        this.assessmentStatus = assessmentStatus;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public void setResultLabel(String resultLabel) {
        this.resultLabel = resultLabel;
    }

    public String getAssessmentAdvice() {
        return assessmentAdvice;
    }

    public void setAssessmentAdvice(String assessmentAdvice) {
        this.assessmentAdvice = assessmentAdvice;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
