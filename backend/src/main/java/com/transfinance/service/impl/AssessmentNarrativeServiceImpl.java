package com.transfinance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.service.AssessmentNarrativeService;
import com.transfinance.vo.AssessmentReportVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估结论与改进路径生成实现。
 * 默认使用规则文本，配置外部 AI 服务后优先调用 AI 生成完整报告文本，避免本地开发依赖密钥。
 */
@Service
public class AssessmentNarrativeServiceImpl implements AssessmentNarrativeService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentNarrativeServiceImpl.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean aiEnabled;
    private final String aiApiUrl;
    private final String aiApiKey;
    private final String aiModel;
    private final boolean responseFormatJson;
    private final boolean deepseekThinkingDisabled;

    public AssessmentNarrativeServiceImpl(RestClient restClient,
                                          ObjectMapper objectMapper,
                                          @Value("${ai.narrative.enabled:false}") boolean aiEnabled,
                                          @Value("${ai.narrative.api-url:}") String aiApiUrl,
                                          @Value("${ai.narrative.api-key:}") String aiApiKey,
                                          @Value("${ai.narrative.model:}") String aiModel,
                                          @Value("${ai.narrative.response-format-json:false}") boolean responseFormatJson,
                                          @Value("${ai.narrative.deepseek-thinking-disabled:false}") boolean deepseekThinkingDisabled) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.aiEnabled = aiEnabled;
        this.aiApiUrl = aiApiUrl;
        this.aiApiKey = aiApiKey;
        this.aiModel = aiModel;
        this.responseFormatJson = responseFormatJson;
        this.deepseekThinkingDisabled = deepseekThinkingDisabled;
    }

    @Override
    public AssessmentReportVO.Narrative generate(AssessmentReportVO report,
                                                 List<AssessmentReportVO.IndicatorItem> allIndicators) {
        AssessmentReportVO.Narrative fallback = buildRuleBasedNarrative(report, allIndicators);
        if (!aiEnabled || !StringUtils.hasText(aiApiUrl) || !StringUtils.hasText(aiApiKey) || !StringUtils.hasText(aiModel)) {
            log.info("AI narrative disabled or incomplete configuration. enabled={}, apiUrlConfigured={}, apiKeyConfigured={}, modelConfigured={}",
                    aiEnabled,
                    StringUtils.hasText(aiApiUrl),
                    StringUtils.hasText(aiApiKey),
                    StringUtils.hasText(aiModel));
            return fallback;
        }
        AssessmentReportVO.Narrative aiNarrative = requestAiNarrative(report, allIndicators);
        if (aiNarrative == null) {
            log.info("AI narrative unavailable, fallback to rule-based narrative.");
            return fallback;
        }
        aiNarrative.setSource("AI生成");
        if (!StringUtils.hasText(aiNarrative.getNote())) {
            aiNarrative.setNote(fallback.getNote());
        }
        log.info("AI narrative generated successfully.");
        return aiNarrative;
    }

    /**
     * 调用兼容 chat-completions 的外部 AI 服务生成结构化报告文本。
     */
    private AssessmentReportVO.Narrative requestAiNarrative(AssessmentReportVO report,
                                                            List<AssessmentReportVO.IndicatorItem> allIndicators) {
        String prompt = buildPrompt(report, allIndicators);
        try {
            log.info("Requesting AI narrative. responseFormatJson={}, deepseekThinkingDisabled={}, modelConfigured={}",
                    responseFormatJson,
                    shouldDisableThinking(),
                    StringUtils.hasText(aiModel));
            return requestAiNarrativeOnce(prompt, responseFormatJson);
        } catch (RestClientResponseException exception) {
            if (responseFormatJson && exception.getStatusCode().is4xxClientError()) {
                log.warn("AI narrative request with response_format failed. status={}, retrying without response_format.",
                        exception.getStatusCode().value());
                try {
                    return requestAiNarrativeOnce(prompt, false);
                } catch (RestClientResponseException retryException) {
                    log.warn("AI narrative retry failed. status={}, response={}",
                            retryException.getStatusCode().value(),
                            abbreviate(retryException.getResponseBodyAsString(), 500));
                    if (retryException.getStatusCode().value() == 400) {
                        log.warn("AI narrative 400 usually means invalid request parameters. Please check whether the model value is Ark Model ID or Endpoint ID, and whether the API URL matches chat completions.");
                    }
                    return null;
                } catch (RestClientException retryException) {
                    log.warn("AI narrative retry failed before response: {}", retryException.getClass().getSimpleName());
                    return null;
                }
            }
            log.warn("AI narrative request failed. status={}, response={}",
                    exception.getStatusCode().value(),
                    abbreviate(exception.getResponseBodyAsString(), 500));
            return null;
        } catch (RestClientException exception) {
            log.warn("AI narrative request failed before response: {}", exception.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 执行一次外部 AI 请求。includeResponseFormat 单独作为参数，是为了兼容部分模型
     * 不支持 response_format 时可以自动重试，而不是直接回退到规则文本。
     */
    private AssessmentReportVO.Narrative requestAiNarrativeOnce(String prompt, boolean includeResponseFormat) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", aiModel);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 1200);
        if (includeResponseFormat) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        if (shouldDisableThinking()) {
            payload.put("thinking", Map.of("type", "disabled"));
        }
        payload.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "你是企业转型金融评估报告分析助手。只输出合法 JSON，不输出 Markdown，不编造未提供的数据。"
                ),
                Map.of(
                        "role", "user",
                        "content", prompt
                )
        ));
        Map<?, ?> response = restClient.post()
                .uri(aiApiUrl)
                .header("Authorization", "Bearer " + aiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
        return parseAiNarrative(extractChatContent(response));
    }

    /**
     * DeepSeek V4 系列模型在部分网关中默认开启思考过程；报告生成只需要最终文本。
     * 自动关闭 thinking 可以减少结构化 JSON 生成失败和参数兼容问题。
     */
    private boolean shouldDisableThinking() {
        return deepseekThinkingDisabled;
    }

    @SuppressWarnings("unchecked")
    private String extractChatContent(Map<?, ?> response) {
        if (response == null) {
            return "";
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return "";
        }
        Object messageValue = choice.get("message");
        if (messageValue instanceof Map<?, ?> message && message.get("content") != null) {
            return message.get("content").toString();
        }
        Object textValue = choice.get("text");
        return textValue == null ? "" : textValue.toString();
    }

    private String buildPrompt(AssessmentReportVO report, List<AssessmentReportVO.IndicatorItem> allIndicators) {
        AssessmentReportVO.BasicInfo info = report.getBasicInfo();
        List<Map<String, Object>> topWeaknesses = allIndicators.stream()
                .sorted(Comparator.comparing(AssessmentReportVO.IndicatorItem::getNormalizedScore))
                .limit(5)
                .map(this::toPromptItem)
                .toList();
        List<Map<String, Object>> topContributors = allIndicators.stream()
                .sorted(Comparator.comparing(AssessmentReportVO.IndicatorItem::getContribution).reversed())
                .limit(5)
                .map(this::toPromptItem)
                .toList();
        Map<String, Object> promptData = new HashMap<>();
        promptData.put("companyCode", info.getCompanyCode());
        promptData.put("industryName", info.getIndustryName());
        promptData.put("companyType", info.getCompanyType());
        promptData.put("dataYear", info.getDataYear());
        promptData.put("transitionScore", info.getTransitionScore());
        promptData.put("modelSource", report.getModelSource());
        promptData.put("topWeaknesses", topWeaknesses);
        promptData.put("topContributors", topContributors);
        try {
            return """
                    请基于以下结构化结果生成转型金融评估报告文本。
                    输出 JSON 格式必须为：
                    {
                      "summary": "一段300到450字的综合结论",
                      "weaknesses": ["3到5条不足分析"],
                      "suggestions": ["3到5条改进建议"],
                      "note": "一段报告说明"
                    }
                    要求：
                    1. 结合转型评分、指标标准化得分、模型权重和贡献值解释原因。
                    2. 不足分析优先围绕低分且有权重的指标。
                    3. 建议要可执行，体现企业转型改进和金融机构辅助评估语境。
                    4. 不输出企业没有提供的事实，不输出与本报告无关的信息。
                    5. 企业主体仅使用 companyCode 企业编码或“该企业”指代，不输出、推断或补写企业真实名称。
                    结构化结果：
                    """
                    + objectMapper.writeValueAsString(promptData);
        } catch (Exception exception) {
            return """
                    请输出合法 JSON，字段包括 summary、weaknesses、suggestions、note，
                    内容为转型金融评估结论与改进路径。
                    """;
        }
    }

    private AssessmentReportVO.Narrative parseAiNarrative(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            String jsonText = extractJsonObject(stripCodeFence(content.trim()));
            Map<?, ?> data = objectMapper.readValue(jsonText, Map.class);
            String summary = valueAsText(data.get("summary"));
            List<String> weaknesses = valueAsStringList(data.get("weaknesses"));
            List<String> suggestions = valueAsStringList(data.get("suggestions"));
            if (!StringUtils.hasText(summary) || weaknesses.isEmpty() || suggestions.isEmpty()) {
                log.warn("AI narrative JSON missing required fields.");
                return null;
            }
            AssessmentReportVO.Narrative narrative = new AssessmentReportVO.Narrative();
            narrative.setSummary(summary);
            narrative.setWeaknesses(weaknesses);
            narrative.setSuggestions(suggestions);
            narrative.setNote(valueAsText(data.get("note")));
            return narrative;
        } catch (Exception exception) {
            log.warn("AI narrative response cannot be parsed as expected JSON: {}", abbreviate(content, 500));
            return null;
        }
    }

    /**
     * 有些模型即使被要求输出 JSON，也可能在 JSON 前后补充说明文字。
     * 这里截取最外层 JSON 对象，提高对不同 chat-completions 服务的兼容性。
     */
    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String stripCodeFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }
        String withoutStart = content.replaceFirst("^```(?:json)?\\s*", "");
        return withoutStart.replaceFirst("\\s*```$", "");
    }

    private String valueAsText(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private List<String> valueAsStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null && StringUtils.hasText(item.toString())) {
                result.add(item.toString().trim());
            }
        }
        return result;
    }

    private Map<String, Object> toPromptItem(AssessmentReportVO.IndicatorItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("indicator", item.getLabel());
        data.put("score", item.getNormalizedScore());
        data.put("weight", item.getWeight());
        data.put("contribution", item.getContribution());
        return data;
    }

    /**
     * 本地规则文本作为稳定兜底，保证未配置外部 AI 时报告仍可生成。
     */
    private AssessmentReportVO.Narrative buildRuleBasedNarrative(AssessmentReportVO report,
                                                                 List<AssessmentReportVO.IndicatorItem> allIndicators) {
        AssessmentReportVO.BasicInfo info = report.getBasicInfo();
        BigDecimal score = info.getTransitionScore() == null ? BigDecimal.ZERO : info.getTransitionScore();
        String level = score.compareTo(BigDecimal.valueOf(80)) >= 0
                ? "良好"
                : score.compareTo(BigDecimal.valueOf(60)) >= 0 ? "可改进" : "偏弱";
        List<AssessmentReportVO.IndicatorItem> weaknesses = allIndicators.stream()
                .sorted(Comparator.comparing(AssessmentReportVO.IndicatorItem::getNormalizedScore))
                .limit(5)
                .toList();
        List<AssessmentReportVO.IndicatorItem> strengths = allIndicators.stream()
                .sorted(Comparator.comparing(AssessmentReportVO.IndicatorItem::getContribution).reversed())
                .limit(3)
                .toList();

        AssessmentReportVO.Narrative narrative = new AssessmentReportVO.Narrative();
        narrative.setSummary("综合评分 " + score.setScale(1, RoundingMode.HALF_UP)
                + " / 100，企业转型金融表现整体处于" + level + "区间。"
                + "从模型权重与指标得分看，"
                + joinLabels(strengths)
                + "对本次评分形成主要支撑；后续应重点关注"
                + joinLabels(weaknesses)
                + "等短板，以提升转型贷款可持续性和授信后的风险可解释性。");
        narrative.setWeaknesses(weaknesses.stream()
                .map(item -> item.getLabel() + "标准化得分为"
                        + item.getNormalizedScore().setScale(1, RoundingMode.HALF_UP)
                        + "分，当前表现低于报告内其他指标。")
                .toList());
        narrative.setSuggestions(weaknesses.stream()
                .map(item -> "围绕" + item.getLabel()
                        + "制定阶段性改进目标，明确责任部门、投入安排和披露频率，并在后续年度数据中持续跟踪。")
                .toList());
        narrative.setNote("本报告由转型金融评估平台依据行业指标体系、当前金融机构模型权重和待评估企业数据生成。报告结论仅用于授信评估辅助，不构成投资建议。");
        narrative.setSource("规则生成");
        return narrative;
    }

    private String joinLabels(List<AssessmentReportVO.IndicatorItem> items) {
        if (items == null || items.isEmpty()) {
            return "核心指标";
        }
        return items.stream()
                .map(AssessmentReportVO.IndicatorItem::getLabel)
                .reduce((left, right) -> left + "、" + right)
                .orElse("核心指标");
    }
}
