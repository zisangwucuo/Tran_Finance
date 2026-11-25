package com.transfinance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 企业评估数据分页查询请求。
 */
public class ElectricCompanyDataQueryRequest {

    /** 当前页码，从1开始。 */
    @Min(value = 1, message = "不能小于1")
    private Long page = 1L;

    /** 每页数量。 */
    @Min(value = 1, message = "不能小于1")
    @Max(value = 100, message = "不能超过100")
    private Long size = 10L;

    /** 当前登录金融机构编码，用于限制只能查看本机构数据。 */
    @NotBlank(message = "不能为空")
    private String institutionId;

    /** 行业键，对应后端白名单中的八个行业表。 */
    @NotBlank(message = "不能为空")
    private String industryKey = "power";

    /** 企业编码模糊查询。 */
    private String companyCode;

    /** 企业名称模糊查询，保留给兼容旧请求。 */
    private String companyName;

    /** 股票代码模糊查询。 */
    private String stockCode;

    /** 数据年份精确查询。 */
    private Integer dataYear;

    /** 经核验转型绩效：1为是，0为否。 */
    private Integer verifiedTransitionPerformance;

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getIndustryKey() {
        return industryKey;
    }

    public void setIndustryKey(String industryKey) {
        this.industryKey = industryKey;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
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

    public Integer getDataYear() {
        return dataYear;
    }

    public void setDataYear(Integer dataYear) {
        this.dataYear = dataYear;
    }

    public Integer getVerifiedTransitionPerformance() {
        return verifiedTransitionPerformance;
    }

    public void setVerifiedTransitionPerformance(Integer verifiedTransitionPerformance) {
        this.verifiedTransitionPerformance = verifiedTransitionPerformance;
    }
}
