package com.transfinance.service;

import com.transfinance.entity.Company;
import com.transfinance.exception.BusinessException;
import com.transfinance.support.CompanyIdentityResolver;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 缺代码、冲突及精确匹配回归测试，避免初始化再次生成随机后缀企业。 */
class CompanyIdentityResolverTest {
    private Company company(String code, String stock) {
        var value = new Company(); value.setCompanyCode(code); value.setStockCode(stock); return value;
    }
    @Test void missingStockReusesUniqueKnownCompany() {
        var existing = company("demo", "601898");
        for (String missing : new String[]{null, "", " ", "-"})
            assertSame(existing, CompanyIdentityResolver.resolve(List.of(existing), missing));
    }
    @Test void ambiguousMissingStockIsNotArbitrarilyAssigned() {
        var candidates = List.of(company("one", "601898"), company("two", "-"));
        assertThrows(BusinessException.class, () -> CompanyIdentityResolver.resolve(candidates, null));
        assertSame(candidates.get(0), CompanyIdentityResolver.resolve(candidates, "601898"));
    }
    @Test void conflictingKnownStockRejectsAutoCreation() {
        assertThrows(BusinessException.class, () -> CompanyIdentityResolver.resolve(List.of(company("one", "601898")), "600985"));
    }
    @Test void missingMasterCanBeReusedAndNoCandidateAllowsCreation() {
        var existing = company("one", "-");
        assertSame(existing, CompanyIdentityResolver.resolve(List.of(existing), "601898"));
        assertNull(CompanyIdentityResolver.resolve(List.of(), null));
    }
}
