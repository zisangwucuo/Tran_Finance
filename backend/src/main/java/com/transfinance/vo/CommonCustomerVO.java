package com.transfinance.vo;

/** 只返回本方身份与安全比较结果；禁止携带对方指标、记录ID和字段摘要。 */
public record CommonCustomerVO(String companyCode, String companyName, Integer year,
        String otherInstitution, String otherInstitutionName, String status,
        java.util.List<String> differentFields, java.util.List<String> differentFieldLabels) {}
