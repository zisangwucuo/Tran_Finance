package com.transfinance.service;

import com.transfinance.controller.CompanyReviewMarkController;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.CompanyReviewMarkMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证标记的会话边界，防止匿名用户、其他银行或任意企业编码写入。 */
class CompanyReviewMarkTest {
    @Test void onlyVerifiedCompaniesFromCurrentBankCanBeMarked() {
        var service = mock(CompanyReviewMarkService.class);
        var controller = new CompanyReviewMarkController(service);
        var request = new MockHttpServletRequest();
        var body = new CompanyReviewMarkController.MarkRequest("demo");
        assertThrows(BusinessException.class, () -> controller.mark(body, request));
        request.getSession().setAttribute("financialInstitutionId", "bank_a");
        request.getSession().setAttribute("psiMarkCandidates:bank_b", Set.of("demo"));
        assertThrows(BusinessException.class, () -> controller.mark(body, request));
        verifyNoInteractions(service);
        request.getSession().setAttribute("psiMarkCandidates:bank_a", Set.of("demo"));
        controller.mark(body, request);
        verify(service).mark("bank_a", "demo");
        assertThrows(BusinessException.class, () -> controller.mark(new CompanyReviewMarkController.MarkRequest("other"), request));
    }

    @Test void readsAndWritesAlwaysCarryInstitutionAndInitializeOnce() {
        var mapper = mock(CompanyReviewMarkMapper.class);
        var service = new CompanyReviewMarkService(mapper);
        service.findCodes("bank_a");
        service.mark("bank_a", "demo");
        service.findCodes("bank_b");
        verify(mapper, times(1)).ensureTable();
        verify(mapper).findCodes("bank_a");
        verify(mapper).findCodes("bank_b");
        verify(mapper).mark("bank_a", "demo");
    }
}
