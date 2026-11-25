package com.transfinance.vo;

import java.util.List;
import java.math.BigDecimal;

/** 材料提取结果：原始证据和候选值分开保存，人工采用不会改写AI原始结果。 */
public record MaterialExtractionVO(String id, String fileName, String industryKey, int dataYear,
        String status, int progress, String message, Result result, String createdAt) {
    public record Page(int number, String text) {}
    public record Candidate(String field, String label, String rawValue, BigDecimal value,
            String unit, Integer year, String denominator, String boundary, String reportCompany,
            Integer page, String quote, String status, List<String> reasons, boolean evidenceVerified) {}
    public record Result(String templateVersion, int pageCount, List<Integer> ocrPages,
            List<Integer> searchedPages, List<Candidate> candidates,
            List<com.fasterxml.jackson.databind.JsonNode> extractedFacts,
            List<com.fasterxml.jackson.databind.JsonNode> aiResponses, String diagnostic) {
        /** 兼容原有任务和旧版测试；旧结果不会被当成新提取事实。 */
        public Result(String version, int count, List<Integer> ocr, List<Integer> searched, List<Candidate> candidates) {
            this(version,count,ocr,searched,candidates,List.of(),List.of(),"");
        }
    }
}
