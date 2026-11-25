package com.transfinance.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 转型金融评估报告展示对象。
 */
public class AssessmentReportVO {

    /** 报告编号。 */
    private String reportNo;

    /** 模型来源，本机构模型或联邦模型。 */
    private String modelSource;

    /** 报告基础信息。 */
    private BasicInfo basicInfo;

    /** 通用指标得分与贡献值。 */
    private List<IndicatorItem> commonIndicators;

    /** 行业专项指标得分与贡献值。 */
    private List<IndicatorItem> specialtyIndicators;

    /** 评估结论与改进路径。 */
    private Narrative narrative;

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public String getModelSource() {
        return modelSource;
    }

    public void setModelSource(String modelSource) {
        this.modelSource = modelSource;
    }

    public BasicInfo getBasicInfo() {
        return basicInfo;
    }

    public void setBasicInfo(BasicInfo basicInfo) {
        this.basicInfo = basicInfo;
    }

    public List<IndicatorItem> getCommonIndicators() {
        return commonIndicators;
    }

    public void setCommonIndicators(List<IndicatorItem> commonIndicators) {
        this.commonIndicators = commonIndicators;
    }

    public List<IndicatorItem> getSpecialtyIndicators() {
        return specialtyIndicators;
    }

    public void setSpecialtyIndicators(List<IndicatorItem> specialtyIndicators) {
        this.specialtyIndicators = specialtyIndicators;
    }

    public Narrative getNarrative() {
        return narrative;
    }

    public void setNarrative(Narrative narrative) {
        this.narrative = narrative;
    }

    /**
     * 报告基础信息。
     */
    public static class BasicInfo {

        private String companyCode;
        private String industryName;
        private String companyType;
        private Integer dataYear;
        private String institutionName;
        private BigDecimal transitionScore;

        public String getCompanyCode() {
            return companyCode;
        }

        public void setCompanyCode(String companyCode) {
            this.companyCode = companyCode;
        }

        public String getIndustryName() {
            return industryName;
        }

        public void setIndustryName(String industryName) {
            this.industryName = industryName;
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

        public String getInstitutionName() {
            return institutionName;
        }

        public void setInstitutionName(String institutionName) {
            this.institutionName = institutionName;
        }

        public BigDecimal getTransitionScore() {
            return transitionScore;
        }

        public void setTransitionScore(BigDecimal transitionScore) {
            this.transitionScore = transitionScore;
        }
    }

    /**
     * 单个指标在报告中的展示数据。
     */
    public static class IndicatorItem {

        private String prop;
        private String label;
        private Object rawValue;
        private BigDecimal normalizedScore;
        private BigDecimal weight;
        private BigDecimal contribution;

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Object getRawValue() {
            return rawValue;
        }

        public void setRawValue(Object rawValue) {
            this.rawValue = rawValue;
        }

        public BigDecimal getNormalizedScore() {
            return normalizedScore;
        }

        public void setNormalizedScore(BigDecimal normalizedScore) {
            this.normalizedScore = normalizedScore;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public BigDecimal getContribution() {
            return contribution;
        }

        public void setContribution(BigDecimal contribution) {
            this.contribution = contribution;
        }
    }

    /**
     * AI 或规则引擎生成的报告文本。
     */
    public static class Narrative {

        private String summary;
        private List<String> weaknesses;
        private List<String> suggestions;
        private String note;
        private String source;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
