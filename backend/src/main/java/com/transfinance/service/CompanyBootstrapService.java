package com.transfinance.service;

/**
 * 企业基础账号初始化服务。
 */
public interface CompanyBootstrapService {

    void bootstrapCompaniesFromHistory();

    void bootstrapCompaniesFromHistory(String defaultPassword);
}
