package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.dto.MaterialAdoptRequest;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.*;
import com.transfinance.vo.MaterialExtractionVO.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 采用操作必须保护企业归属、并发旧值和证据；失败时事务回滚。 */
class MaterialAdoptionTest {
    MaterialExtractionMapper mapper=mock(MaterialExtractionMapper.class);
    PlatformTransactionManager tx=mock(PlatformTransactionManager.class);
    MaterialExtractionService service;
    @BeforeEach void setup() throws Exception {
        when(tx.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        ObjectMapper json=new ObjectMapper();
        Candidate c=new Candidate("steelCarbonEmissionIntensity","吨钢碳排放强度","2.00",new BigDecimal("2.00"),
                "吨二氧化碳/吨粗钢",2024,"粗钢产量","四基地","示例企业",40,"示例证据","REVIEW",List.of("需核对"),true);
        Map<String,Object> task=new HashMap<>(Map.of("id","task","fileName","sample.pdf","industryKey","steel",
                "dataYear",2024,"status","COMPLETED","progress",100,"message","完成","createdAt","2026-11-09"));
        task.put("resultJson",json.writeValueAsString(new Result("v1",40,List.of(),List.of(40),List.of(c))));
        when(mapper.get("owner","task")).thenReturn(task);
        when(mapper.adoptedRows(anyString(),anyString())).thenReturn(List.of());
        when(mapper.lockTarget("steel_pending_company_data",1L,"owner",2024))
                .thenReturn(new HashMap<>(Map.of("steel_carbon_emission_intensity",new BigDecimal("3"))));
        when(mapper.apply(anyString(),anyLong(),anyString(),anyMap())).thenReturn(1);
        service=new MaterialExtractionService(mapper,mock(CompanyMapper.class),mock(MaterialPdfReader.class),
                mock(MaterialAiClient.class),new MaterialCandidateValidator(),json,tx,"");
    }
    MaterialAdoptRequest request(String value,String old) {
        return new MaterialAdoptRequest(1L,true,List.of(new MaterialAdoptRequest.Item("steelCarbonEmissionIntensity",
                new BigDecimal(value),new BigDecimal(old),"已核对主体与口径")));
    }
    @AfterEach void close() { service.stop(); }
    @Test void validAdoptionWritesAuditAndValuesTogether() {
        service.adopt("owner","task",request("2","3"));
        verify(mapper).audit(eq("task"),eq("owner"),eq("steel_pending_company_data"),eq(1L),contains("reviewNote"));
        verify(mapper).apply(eq("steel_pending_company_data"),eq(1L),contains("steel_carbon_emission_intensity"),anyMap());
        verify(tx).commit(any());
    }
    @Test void concurrentChangeDoesNotOverwrite() {
        assertThrows(BusinessException.class,()->service.adopt("owner","task",request("2","4")));
        verify(mapper,never()).apply(anyString(),anyLong(),anyString(),anyMap()); verify(tx).rollback(any());
    }
    @Test void changingCandidateCannotBypassEvidence() {
        assertThrows(BusinessException.class,()->service.adopt("owner","task",request("1","3")));
        verify(mapper,never()).audit(anyString(),anyString(),anyString(),anyLong(),anyString());
    }
    @Test void anotherEnterpriseCannotAccessTask() {
        assertThrows(BusinessException.class,()->service.adopt("other","task",request("2","3")));
        verify(mapper,never()).lockTarget(anyString(),anyLong(),anyString(),anyInt());
    }
    @Test void deletedOrForeignTargetCannotBeWritten() {
        when(mapper.lockTarget(anyString(),anyLong(),anyString(),anyInt())).thenReturn(null);
        assertThrows(BusinessException.class,()->service.adopt("owner","task",request("2","3")));
        verify(mapper,never()).apply(anyString(),anyLong(),anyString(),anyMap());
    }
}
