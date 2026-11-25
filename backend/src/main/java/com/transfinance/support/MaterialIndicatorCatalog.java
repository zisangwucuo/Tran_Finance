package com.transfinance.support;

import java.util.*;
import com.transfinance.exception.BusinessException;

/**
 * 首版人工配置的评价元模型，不宣称自动解析政策或完成官方标准认证。
 * 模板包含分母和单位，阻止把“吨成品钢”直接当成“吨粗钢”或把降幅当作强度。
 * 标准出处暂为项目指标目录；正式业务应补充经审核的政策条款及排放范围定义。
 */
public final class MaterialIndicatorCatalog {
    private MaterialIndicatorCatalog() {}
    public static final String VERSION = "material-demo-v1";
    public record Indicator(String field, String label, String unit, String numerator,
            String denominator, List<String> keywords, boolean percentage, String source) {}
    private static Indicator item(String industry, String field, String unit, String numerator,
                                  String denominator, String... aliases) {
        var meta = IndustryDataCatalog.requireField(industry, field);
        return new Indicator(field, meta.label(), unit, numerator, denominator,
                List.of(aliases), "%".equals(unit), "项目指标目录 / 演示口径v1，非官方标准认证");
    }
    public static List<Indicator> forIndustry(String industry) {
        if (!Set.of("steel", "chemical").contains(industry))
            throw new BusinessException("首版材料提取支持钢铁、基础化工行业，其他行业暂未配置口径模板");
        List<Indicator> result = new ArrayList<>(List.of(
            item(industry, "salesNetProfitMargin", "%", "净利润", "营业收入", "销售净利率", "净利率"),
            item(industry, "revenueGrowthRate", "%", "本期与上期营业收入差额", "上期营业收入", "营业收入增长", "营业收入同比"),
            item(industry, "rAndDIntensity", "%", "研发投入", "营业收入", "研发投入", "研发费用", "研发强度")
        ));
        if ("steel".equals(industry)) result.addAll(List.of(
            item(industry, "steelCarbonEmissionIntensity", "吨二氧化碳/吨粗钢", "二氧化碳排放量", "粗钢产量", "碳排放强度", "吨钢碳", "吨粗钢"),
            item(industry, "blastFurnaceEnergyConsumption", "kgce/t", "高炉工序能耗", "生铁产量", "高炉工序能耗", "炼铁工序能耗"),
            item(industry, "converterEnergyConsumption", "kgce/t", "转炉工序能耗", "转炉钢产量", "转炉工序能耗", "转炉能耗"),
            item(industry, "electricFurnaceShortProcessRatio", "%", "电炉短流程产量", "粗钢产量", "短流程", "电炉钢占比"),
            item(industry, "byproductGasUtilizationRatio", "%", "已利用煤气量", "可回收煤气量", "煤气利用率", "煤气回收利用率")
        ));
        else result.addAll(List.of(
            item(industry, "greenPowerUseRatio", "%", "绿色电力使用量", "总用电量", "绿电", "绿色电力", "可再生电力"),
            item(industry, "industrialWaterReuseRatio", "%", "工业重复用水量", "工业总用水量", "水重复利用率", "重复用水率"),
            item(industry, "syntheticAmmoniaEnergyConsumption", "kgce/t", "合成氨综合能耗", "合成氨产量", "合成氨", "综合能耗"),
            item(industry, "productCarbonEmissionIntensity", "tCO2/t", "产品二氧化碳排放量", "同类产品产量", "产品碳排放", "单位产品碳", "碳排放强度")
        ));
        return List.copyOf(result);
    }
}
