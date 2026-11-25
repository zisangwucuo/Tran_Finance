package com.transfinance.config;

import com.transfinance.service.CompanyBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 后端启动后自动从历史评估数据补齐企业基础表和默认企业账号。
 */
@Component
public class CompanyBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CompanyBootstrapRunner.class);

    private final CompanyBootstrapService companyBootstrapService;

    public CompanyBootstrapRunner(CompanyBootstrapService companyBootstrapService) {
        this.companyBootstrapService = companyBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            companyBootstrapService.bootstrapCompaniesFromHistory();
        } catch (Exception exception) {
            log.warn("企业基础账号初始化未完成：{}", exception.getClass().getSimpleName());
        }
    }
}
