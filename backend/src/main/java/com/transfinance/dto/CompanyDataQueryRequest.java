package com.transfinance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 企业端历史数据与待评估数据分页查询请求。
 */
public class CompanyDataQueryRequest {

    /** 当前页码，从1开始。 */
    @Min(value = 1, message = "不能小于1")
    private Long page = 1L;

    /** 每页数量。 */
    @Min(value = 1, message = "不能小于1")
    @Max(value = 100, message = "不能超过100")
    private Long size = 10L;

    /** 当前登录企业编码，用于限制只能查看本企业数据。 */
    @NotBlank(message = "不能为空")
    private String companyCode;

    /** 行业筛选，可为空表示全部行业。 */
    private String industryKey;

    /** 年份筛选，可为空表示全部年份。 */
    private Integer dataYear;

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

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getIndustryKey() {
        return industryKey;
    }

    public void setIndustryKey(String industryKey) {
        this.industryKey = industryKey;
    }

    public Integer getDataYear() {
        return dataYear;
    }

    public void setDataYear(Integer dataYear) {
        this.dataYear = dataYear;
    }
}
