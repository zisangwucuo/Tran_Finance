package com.transfinance.service;

import com.transfinance.controller.CustomerOverviewController;
import com.transfinance.entity.FinancialInstitution;
import com.transfinance.exception.GlobalExceptionHandler;
import com.transfinance.mapper.CustomerOverviewMapper;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.service.impl.CustomerOverviewServiceImpl;
import com.transfinance.support.IndustryDataCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 总览口径与会话隔离回归：重复年份不能放大客户数，空评分不能变成零分。 */
class CustomerOverviewTest {
    private final CustomerOverviewMapper mapper = mock(CustomerOverviewMapper.class);
    private final FinancialInstitutionMapper institutions = mock(FinancialInstitutionMapper.class);

    private CustomerOverviewService service() {
        var institution = new FinancialInstitution();
        institution.setInstitutionCode("bank_a");
        institution.setInstitutionName("测试银行");
        when(institutions.selectById("bank_a")).thenReturn(institution);
        when(mapper.selectRecords(anyString(), eq("bank_a"), eq("测试银行"))).thenReturn(List.of());
        when(mapper.selectNoticeCounts(eq("bank_a"), any())).thenReturn(List.of());
        return new CustomerOverviewServiceImpl(mapper, institutions);
    }

    @Test void countsDistinctCustomersAcrossYearsAndDatasetsAndExcludesMissingScores() {
        var service = service();
        when(mapper.selectRecords("power_company_data", "bank_a", "测试银行")).thenReturn(List.of(
                row("A", 2023, "0"), row("A", 2024, "40"), row("B", 2024, null), row(null, 2024, "39.9")));
        when(mapper.selectRecords("steel_pending_company_data", "bank_a", "测试银行")).thenReturn(List.of(
                row("A", 2024, "60"), row("B", 2024, "80"), row("C", 2024, "100"), row("D", 2024, "101")));
        when(mapper.selectNoticeCounts("bank_a", null)).thenReturn(List.of(Map.of("status", "PENDING", "total", 2L)));
        var result = service.overview("bank_a", null);
        assertEquals(4, result.totals().customers());
        assertEquals(8, result.totals().records());
        assertEquals(1, result.totals().unresolvedRecords());
        assertEquals(6, result.totals().scoredRecords());
        assertEquals(new BigDecimal("53.3"), result.totals().averageScore());
        assertEquals(3, result.totals().pendingScored());
        assertEquals(1, result.totals().pendingUnscored());
        assertEquals(List.of(2L, 1L, 1L, 2L), result.scoreBands().stream().map(band -> band.count()).toList());
        assertEquals(List.of(2024, 2023), result.years());
        assertEquals(1, result.yearly().get(0).customers());
        assertEquals(2, result.notices().pending());
        // 同一客户在多个行业出现时各行业分别去重，全行仍只计一次。
        assertEquals(6, result.industries().stream().mapToLong(item -> item.customers()).sum());
        assertEquals(8, result.industries().size());
        for (var industry : IndustryDataCatalog.industries().values()) {
            verify(mapper).selectRecords(industry.tableName(), "bank_a", "测试银行");
            verify(mapper).selectRecords(industry.pendingTableName(), "bank_a", "测试银行");
        }
    }

    @Test void selectedYearFiltersAllStatisticsAndPreservesAvailableYears() {
        var service = service();
        when(mapper.selectRecords("power_company_data", "bank_a", "测试银行")).thenReturn(List.of(
                row("A", 2023, "100"), row("B", 2024, null), row("C", null, "50")));
        var result = service.overview("bank_a", 2024);
        assertEquals(1, result.totals().customers());
        assertEquals(1, result.totals().records());
        assertNull(result.totals().averageScore());
        assertEquals(1, result.totals().unscoredRecords());
        assertEquals(List.of(2024, 2023), result.years());
        assertEquals(1, result.yearly().size());
        verify(mapper).selectNoticeCounts("bank_a", 2024);
    }

    @Test void emptyBankHasNoInventedScoreOrCustomers() {
        var result = service().overview("bank_a", null);
        assertEquals(0, result.totals().customers());
        assertEquals(0, result.totals().coveredIndustries());
        assertNull(result.totals().averageScore());
        assertTrue(result.years().isEmpty());
    }

    @Test void endpointRequiresSessionRejectsInvalidYearAndIgnoresClientBank() throws Exception {
        var service = mock(CustomerOverviewService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CustomerOverviewController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/customer-overview")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(service);
        var session = new MockHttpSession();
        session.setAttribute("financialInstitutionId", "bank_a");
        mvc.perform(get("/api/customer-overview").session(session).param("institutionId", "bank_b").param("dataYear", "2024"))
                .andExpect(status().isOk());
        verify(service).overview("bank_a", 2024);
        mvc.perform(get("/api/customer-overview").session(session).param("dataYear", "1999"))
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(service);
    }

    private Map<String, Object> row(String code, Integer year, String score) {
        var row = new HashMap<String, Object>();
        row.put("companyCode", code);
        row.put("dataYear", year);
        row.put("transitionScore", score == null ? null : new BigDecimal(score));
        return row;
    }
}
