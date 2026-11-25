package com.transfinance.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.transfinance.support.MaterialIndicatorCatalog;
import com.transfinance.vo.MaterialExtractionVO.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 新流程回归：事实不能因模板、年份或主体不匹配而消失，但禁止错误采用。 */
class MaterialFactPipelineTest {
    ObjectMapper json=new ObjectMapper(); MaterialFactPipeline pipeline=new MaterialFactPipeline();
    String quote="2025年示例公司研发投入占主营业务收入比例为3.99%，目标为3.85%。";
    ObjectNode fact() {
        return json.valueToTree(Map.of("name","研发投入占主营业务收入比例","rawValue","3.99%","unit","%",
                "year",2025,"denominator","主营业务收入","boundary","公司","reportCompany","示例公司",
                "valueKind","actual","page",12,"quote",quote));
    }
    List<Candidate> match(List<JsonNode> facts) {
        return pipeline.match(facts,MaterialIndicatorCatalog.forIndustry("steel"),List.of(new Page(12,quote)),2025,"示例公司",new MaterialCandidateValidator());
    }
    @Test void differentDenominatorRemainsVisibleButBlocked() {
        var rows=match(List.of(fact())); var c=rows.get(0);
        assertEquals("研发投入占主营业务收入比例",c.label()); assertEquals("3.99%",c.rawValue());
        assertEquals("rAndDIntensity",c.field()); assertEquals("BLOCKED",c.status());
        assertTrue(c.reasons().stream().anyMatch(s->s.contains("口径不同")));
    }
    @Test void outsideTemplateIsPreserved() {
        var fact=fact(); fact.put("name","员工总人数");
        var c=match(List.of(fact)).get(0); assertEquals("OUTSIDE",c.status()); assertEquals("",c.field());
    }
    @Test void wrongYearIsNotDropped() {
        var fact=fact(); fact.put("year",2024); var c=match(List.of(fact)).get(0);
        assertEquals(2024,c.year()); assertEquals("BLOCKED",c.status());
    }
    @Test void targetIsNotActual() {
        var fact=fact(); fact.put("denominator","营业收入"); fact.put("valueKind","target");
        assertTrue(match(List.of(fact)).get(0).reasons().stream().anyMatch(s->s.contains("target")));
    }
    @Test void subjectMismatchCannotBeAdopted() {
        var fact=fact(); fact.put("denominator","营业收入"); fact.put("reportCompany","其他公司");
        assertEquals("BLOCKED",match(List.of(fact)).get(0).status());
    }
    @Test void extractionPromptDoesNotReceiveTargetTemplateOrCompany() throws Exception {
        String prompt=pipeline.prompt(List.of(new Page(12,quote)));
        assertFalse(prompt.contains("rAndDIntensity")); assertFalse(prompt.contains("targetYear"));
        assertTrue(prompt.contains("不做评价模板筛选"));
    }
    @Test void duplicateBatchesDeduplicateWithoutLosingDifferentYears() {
        var second=fact(); second.put("year",2024);
        var one=json.createObjectNode(); one.putArray("candidates").add(fact());
        var two=json.createObjectNode(); two.putArray("candidates").add(fact()).add(second);
        assertEquals(2,pipeline.facts(List.of(one,two)).size());
    }
    @Test void oldResultJsonStillLoads() throws Exception {
        var result=json.readValue("{\"templateVersion\":\"v1\",\"pageCount\":1,\"ocrPages\":[],\"searchedPages\":[1],\"candidates\":[]}",Result.class);
        assertEquals(1,result.pageCount()); assertNull(result.aiResponses());
    }
}
