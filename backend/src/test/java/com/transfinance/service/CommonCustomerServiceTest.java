package com.transfinance.service;
import com.transfinance.controller.CommonCustomerController;
import com.transfinance.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** 验证匿名访问和伪造机构参数均不能获得其他银行的求交结果。 */
class CommonCustomerServiceTest {
    @Test void rejectsAnonymousRequest() {
        var service = mock(CommonCustomerService.class);
        var controller = new CommonCustomerController(service);
        var scope = new CommonCustomerController.Scope("power", "history");
        assertThrows(BusinessException.class, () -> controller.find(scope, new MockHttpServletRequest()));
        verifyNoInteractions(service);
    }
    @Test void usesSessionInsteadOfClientInstitution() {
        var service = mock(CommonCustomerService.class);
        var controller = new CommonCustomerController(service);
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute("financialInstitutionId", "bank_a");
        request.setParameter("institutionId", "bank_b");
        when(service.find("bank_a", "power", "history"))
                .thenReturn(new CommonCustomerService.Result(List.of(), 0, 0, List.of()));
        controller.find(new CommonCustomerController.Scope("power", "history"), request);
        verify(service).find("bank_a", "power", "history");
        verifyNoMoreInteractions(service);
    }
}
