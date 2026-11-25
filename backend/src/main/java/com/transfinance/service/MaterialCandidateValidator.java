package com.transfinance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.transfinance.support.MaterialIndicatorCatalog.Indicator;
import com.transfinance.vo.MaterialExtractionVO.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

/** 独立规则校验，不采用AI自行声称的“置信度”或“校验通过”。 */
@Component
public class MaterialCandidateValidator {
    public static String compact(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).replaceAll("\\s+", "");
    }
    public List<Candidate> validate(JsonNode response, List<Indicator> template, List<Page> pages, int year, String company) {
        List<Candidate> results = new ArrayList<>();
        Map<Integer,String> source = new HashMap<>();
        pages.forEach(page -> source.put(page.number(), compact(page.text())));
        for (Indicator field : template) {
            List<JsonNode> matches = new ArrayList<>();
            response.path("candidates").forEach(node -> { if (field.field().equals(node.path("field").asText())) matches.add(node); });
            if (matches.isEmpty()) {
                results.add(new Candidate(field.field(), field.label(), "", null, "", null, "", "", "", null, "",
                        "MISSING", List.of("所检索页面未找到有依据的值，不代表企业未实施或取值为0"), false));
                continue;
            }
            // 同一指标多候选不默默覆盖；保留首候选并明确阻止直接采用，等待重新拆分材料。
            JsonNode node = matches.get(0);
            String quote = value(node,"quote"), raw = value(node,"rawValue"), unit = value(node,"unit");
            String denominator = value(node,"denominator"), boundary = value(node,"boundary"), subject = value(node,"reportCompany");
            int page = node.path("page").asInt(-1), actualYear = node.path("year").asInt(-1);
            boolean evidence = compact(quote).length() >= 12 && source.getOrDefault(page, "").contains(compact(quote));
            List<String> reasons = new ArrayList<>();
            boolean blocked = false;
            if (!evidence) { reasons.add("页码或原文片段无法核验"); blocked = true; }
            BigDecimal number = null;
            try {
                String numeric = compact(raw).replace(",", "").replace("%", "");
                if (!numeric.matches("-?\\d+(\\.\\d+)?")) throw new NumberFormatException();
                number = new BigDecimal(numeric);
                if (number.precision() > 16 || number.scale() > 6) throw new NumberFormatException();
                // 数值必须是独立token，避免把原文12.00错误核验为2.00。
                String token = compact(raw).replace("%", "");
                if (token.isEmpty() || !java.util.regex.Pattern.compile("(?<![\\d.,])" + java.util.regex.Pattern.quote(token) + "(?![\\d.,])")
                        .matcher(compact(quote)).find()) {
                    reasons.add("原文中无法核验该原始数值"); blocked = true;
                }
            } catch (NumberFormatException e) { reasons.add("候选值不是可直接采用的数值"); blocked = true; number = null; }
            if (actualYear != year) { reasons.add("候选年份与目标年份不一致或未明确"); blocked = true; }
            else if (!compact(quote).contains(String.valueOf(year))) reasons.add("年份不在证据片段内，需核对表头或报告期间");
            if (matches.size() > 1) { reasons.add("同一指标存在多个候选，请拆分或核实冲突后重新提取"); blocked = true; }
            if (!compact(subject).equals(compact(company))) reasons.add("报告主体与登录企业名称不完全一致，需确认简称、子公司或集团范围");
            if (boundary.isBlank()) reasons.add("核算边界未披露");
            else reasons.add("请人工确认核算边界与本次评价企业一致：" + boundary);
            if (field.percentage()) {
                if (!Set.of("%", "百分比").contains(compact(unit))) { reasons.add("比例单位不是百分数，不能直接填入"); blocked = true; }
                // 利润率和收入增速可以为负，增速也可能超过100%；其他占比限定0—100。
                if (number != null && !Set.of("salesNetProfitMargin", "revenueGrowthRate").contains(field.field())
                        && (number.signum() < 0 || number.compareTo(new BigDecimal("100")) > 0)) {
                    reasons.add("占比超出0至100范围"); blocked = true;
                }
            } else if (field.field().equals("steelCarbonEmissionIntensity")) {
                if (!compact(unit).contains("粗钢") || !compact(denominator).contains("粗钢")) {
                    reasons.add("分母不是已确认的粗钢产量，不能把成品钢口径直接转换"); blocked = true;
                }
                if (!compact(unit).equals(compact(field.unit()))) { reasons.add("单位与模板不完全一致，首版不自动换算"); blocked = true; }
            } else if (!compact(unit).equals(compact(field.unit()))) {
                reasons.add("单位与模板不完全一致，首版不自动换算"); blocked = true;
            }
            if (denominator.isBlank()) reasons.add("分母口径未明确，需人工复核");
            else if (!compact(denominator).equals(compact(field.denominator()))) reasons.add("分母表述与模板不同，需核对实际口径");
            results.add(new Candidate(field.field(), field.label(), raw, number, unit, actualYear < 0 ? null : actualYear,
                    denominator, boundary, subject, page < 1 ? null : page, quote,
                    blocked ? "BLOCKED" : reasons.isEmpty() ? "READY" : "REVIEW", List.copyOf(reasons), evidence));
        }
        return results;
    }
    private String value(JsonNode node, String key) {
        String text = node.path(key).asText("").trim();
        return text.substring(0, Math.min(text.length(), 2500));
    }
}
