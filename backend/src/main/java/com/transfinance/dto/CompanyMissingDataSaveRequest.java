package com.transfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 企业补全缺失字段请求。
 */
public class CompanyMissingDataSaveRequest {

    @NotBlank(message = "不能为空")
    private String companyCode;

    @NotNull(message = "不能为空")
    private Map<String, Object> values;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}
