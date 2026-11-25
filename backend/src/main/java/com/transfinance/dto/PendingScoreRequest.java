package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 待评估企业模型评分请求。
 */
public class PendingScoreRequest {

    /** 当前登录金融机构编码。 */
    @NotBlank(message = "不能为空")
    private String institutionId;

    /** 行业键。 */
    @NotBlank(message = "不能为空")
    private String industryKey;

    /** 评分模型模式：local 表示本机构模型，federated 表示联邦行业模型。 */
    private String modelMode;

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

    public String getModelMode() {
        return modelMode;
    }

    public void setModelMode(String modelMode) {
        this.modelMode = modelMode;
    }
}
