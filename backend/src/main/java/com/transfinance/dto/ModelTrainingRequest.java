package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 模型训练请求。
 */
public class ModelTrainingRequest {

    /** 仅更新训练采用自定义参数；旧客户端不传时保持默认配置。 */
    @jakarta.validation.Valid
    private ModelParameters parameters = ModelParameters.defaults();

    public ModelParameters getParameters() { return parameters; }
    public void setParameters(ModelParameters parameters) { this.parameters = parameters; }

    /** 当前登录金融机构编码。 */
    @NotBlank(message = "不能为空")
    private String institutionId;

    /** 是否在存在缺失数据时强行训练。 */
    private Boolean force = false;

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }
}
