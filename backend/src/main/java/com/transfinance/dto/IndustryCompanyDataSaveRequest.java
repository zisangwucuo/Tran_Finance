package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 分行业企业评估数据新增/修改请求。
 */
public class IndustryCompanyDataSaveRequest {

    /** 当前登录金融机构编码。 */
    @NotBlank(message = "不能为空")
    private String institutionId;

    /** 行业键，对应后端白名单中的八个行业表。 */
    @NotBlank(message = "不能为空")
    private String industryKey;

    /** 原行业键，修改时用于支持跨行业迁移。 */
    private String sourceIndustryKey;

    /** 企业评估数据字段值，字段名使用前端列元数据中的 prop。 */
    @NotNull(message = "不能为空")
    private Map<String, Object> data;

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

    public String getSourceIndustryKey() {
        return sourceIndustryKey;
    }

    public void setSourceIndustryKey(String sourceIndustryKey) {
        this.sourceIndustryKey = sourceIndustryKey;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
