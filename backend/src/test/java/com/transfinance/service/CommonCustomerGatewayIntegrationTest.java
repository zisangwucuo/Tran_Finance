package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.controller.CommonCustomerController;
import com.transfinance.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 可选真实节点联调：mvn test -Dpsi.integration=true。
 * 不启动Spring数据库初始化器，不修改业务数据；仅加载网关及控制器访问已启动的节点。
 */
@EnabledIfSystemProperty(named = "psi.integration", matches = "true")
class CommonCustomerGatewayIntegrationTest {
    @Test void authenticatedGatewayRunsRealPsiAndRejectsAnonymousRequest() throws Exception {
        var json = new ObjectMapper();
        var service = new CommonCustomerService(json);
        assertEquals(3, service.status().stream().filter(n -> "READY".equals(n.status())).count());
        var mvc = MockMvcBuilders.standaloneSetup(new CommonCustomerController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/common-customers/status"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(401));
        var session = new MockHttpSession();
        session.setAttribute("financialInstitutionId", "bank_a");
        var response = mvc.perform(post("/api/common-customers").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"industryKey\":\"power\",\"dataset\":\"history\",\"institutionId\":\"bank_b\"}"))
                .andReturn();
        assertEquals(200, response.getResponse().getStatus(),
                json.readTree(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("message").asText());
        var data = json.readTree(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data");
        assertEquals(2, data.path("checkedPeers").size());
        for (var peer : data.path("checkedPeers")) assertNotEquals("bank_a", peer.asText());
        for (var row : data.path("records")) {
            assertNotEquals("bank_a", row.path("otherInstitution").asText());
            assertFalse(row.has("differences"));
            assertEquals(8, row.size());
            assertTrue(row.path("differentFields").isArray());
            assertEquals(row.path("differentFields").size(), row.path("differentFieldLabels").size());
        }
    }
}
