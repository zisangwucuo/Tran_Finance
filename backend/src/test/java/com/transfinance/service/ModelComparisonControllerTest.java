package com.transfinance.service;

import com.transfinance.controller.ModelComparisonController;
import com.transfinance.dto.ModelComparisonRequest;
import com.transfinance.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 会话隔离回归：未登录不可用，身份只能来自会话而非请求参数。 */
class ModelComparisonControllerTest {
    @Test void requiresSessionAndUsesSessionBank() {
        var service=mock(ModelComparisonService.class);
        var controller=new ModelComparisonController(service);
        var request=new MockHttpServletRequest();
        assertThrows(BusinessException.class,()->controller.list(request));
        request.getSession().setAttribute("financialInstitutionId","bank_a");
        request.setParameter("institutionId","bank_b");
        controller.start(new ModelComparisonRequest("power"),request);
        verify(service).start("bank_a","power","default");
        verifyNoMoreInteractions(service);
    }
}
