package com.transfinance.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.entity.FinancialInstitution;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.FederatedLearningMapper;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.mapper.ModelTrainingMapper;
import com.transfinance.service.AssessmentNarrativeService;
import com.transfinance.service.AssessmentReportService;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.vo.AssessmentReportVO;
import com.transfinance.vo.FederatedModelVO;
import com.transfinance.vo.IndustryModelVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 转型金融评估报告服务实现。
 */
@Service
public class AssessmentReportServiceImpl implements AssessmentReportService {

    private static final Set<String> COMMON_PROPS = Set.of(
            "transitionTargetCommitment",
            "transitionStrategyPath",
            "governanceIncentive",
            "transitionTechnologyApplication",
            "carbonAssetManagement",
            "riskManagementMarketOpportunity",
            "informationDisclosureTransparency",
            "justTransition",
            "environmentalCompliance",
            "salesNetProfitMargin",
            "revenueGrowthRate",
            "rAndDIntensity"
    );

    private static final Map<String, IndicatorScale> SCALES = createScales();

    private final JdbcTemplate jdbcTemplate;
    private final FinancialInstitutionMapper institutionMapper;
    private final ModelTrainingMapper modelTrainingMapper;
    private final FederatedLearningMapper federatedLearningMapper;
    private final AssessmentNarrativeService narrativeService;
    private final ObjectMapper objectMapper;

    public AssessmentReportServiceImpl(JdbcTemplate jdbcTemplate,
                                       FinancialInstitutionMapper institutionMapper,
                                       ModelTrainingMapper modelTrainingMapper,
                                       FederatedLearningMapper federatedLearningMapper,
                                       AssessmentNarrativeService narrativeService,
                                       ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.institutionMapper = institutionMapper;
        this.modelTrainingMapper = modelTrainingMapper;
        this.federatedLearningMapper = federatedLearningMapper;
        this.narrativeService = narrativeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AssessmentReportVO buildPendingReport(String institutionId, String industryKey, Long id, String modelMode) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDataCatalog.IndustryMeta industry = IndustryDataCatalog.requireIndustry(industryKey);
        String normalizedModelMode = StringUtils.hasText(modelMode) ? modelMode : "local";
        Map<String, Object> row = selectPendingRow(industry, institution, id);
        if (row == null) {
            throw new BusinessException("待评估企业数据不存在或无权生成报告");
        }
        if (row.get("transitionScore") == null) {
            throw new BusinessException("请先完成模型评估，再生成报告");
        }

        Map<String, BigDecimal> weights = resolveFeatureWeights(institution.getInstitutionCode(), industry.key(), normalizedModelMode);
        List<AssessmentReportVO.IndicatorItem> commonItems = buildIndicatorItems(industry.key(), row, industry.featureFields(), weights, true);
        List<AssessmentReportVO.IndicatorItem> specialtyItems = buildIndicatorItems(industry.key(), row, industry.featureFields(), weights, false);
        List<AssessmentReportVO.IndicatorItem> allItems = new ArrayList<>();
        allItems.addAll(commonItems);
        allItems.addAll(specialtyItems);

        AssessmentReportVO report = new AssessmentReportVO();
        report.setReportNo(buildReportNo(industry.key(), id, row.get("dataYear")));
        report.setModelSource("federated".equals(normalizedModelMode) ? "联邦模型" : "本机构模型");
        report.setBasicInfo(buildBasicInfo(row, industry.name(), institution.getInstitutionName()));
        report.setCommonIndicators(commonItems);
        report.setSpecialtyIndicators(specialtyItems);
        report.setNarrative(narrativeService.generate(report, allItems));
        redactCompanyName(report.getNarrative(), asString(row.get("companyName")), report.getBasicInfo().getCompanyCode());
        return report;
    }

    /**
     * 对AI或规则生成的全部正文做最终替换；打印页面复用同一响应，避免名称进入PDF。
     * 企业名称只用于内部比对，不进入报告基本信息或AI输入。
     */
    private void redactCompanyName(AssessmentReportVO.Narrative narrative, String companyName, String companyCode) {
        if (narrative == null || !StringUtils.hasText(companyName)) {
            return;
        }
        String replacement = StringUtils.hasText(companyCode) ? companyCode : "该企业";
        if (narrative.getSummary() != null) {
            narrative.setSummary(narrative.getSummary().replace(companyName, replacement));
        }
        if (narrative.getNote() != null) {
            narrative.setNote(narrative.getNote().replace(companyName, replacement));
        }
        if (narrative.getWeaknesses() != null) {
            narrative.setWeaknesses(narrative.getWeaknesses().stream()
                    .map(text -> text == null ? null : text.replace(companyName, replacement)).toList());
        }
        if (narrative.getSuggestions() != null) {
            narrative.setSuggestions(narrative.getSuggestions().stream()
                    .map(text -> text == null ? null : text.replace(companyName, replacement)).toList());
        }
    }

    private Map<String, Object> selectPendingRow(IndustryDataCatalog.IndustryMeta industry,
                                                 FinancialInstitution institution,
                                                 Long id) {
        String selectColumns = buildSelectColumns(industry);
        String sql = """
                select %s
                  from %s t
                 where t.id = ?
                   and (
                         t.institution_code = ?
                         or (coalesce(t.institution_code, '') = '' and t.institution_name = ?)
                       )
                """.formatted(selectColumns, industry.pendingTableName());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql,
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 只拼接后端白名单中的表名和字段，避免外部输入进入动态 SQL。
     */
    private String buildSelectColumns(IndustryDataCatalog.IndustryMeta industry) {
        List<String> expressions = new ArrayList<>();
        expressions.add("t.id as `id`");
        expressions.add("t.company_name as `companyName`");
        expressions.add("t.stock_code as `stockCode`");
        expressions.add("t.company_type as `companyType`");
        expressions.add("t.data_year as `dataYear`");
        expressions.add("t.industry_name as `industryName`");
        expressions.add("t.transition_score as `transitionScore`");
        expressions.add("""
                (
                  select c.company_code
                    from company c
                   where c.company_name = t.company_name
                     and (
                           c.stock_code = t.stock_code
                           or (c.stock_code is null and t.stock_code is null)
                           or (c.stock_code = '-' and t.stock_code is null)
                           or (c.stock_code = '-' and t.stock_code = '')
                           or (c.stock_code is null and t.stock_code = '-')
                           or (c.stock_code = '' and t.stock_code = '-')
                           or coalesce(t.stock_code, '') = ''
                         )
                   order by case when c.industry_name = t.industry_name then 0 else 1 end
                   limit 1
                ) as `companyCode`
                """.replaceAll("\\s+", " ").trim());
        expressions.addAll(industry.featureFields().stream()
                .map(field -> "t.`" + field.column() + "` as `" + field.prop() + "`")
                .toList());
        return String.join(", ", expressions);
    }

    private AssessmentReportVO.BasicInfo buildBasicInfo(Map<String, Object> row,
                                                        String industryName,
                                                        String institutionName) {
        AssessmentReportVO.BasicInfo info = new AssessmentReportVO.BasicInfo();
        info.setCompanyCode(asString(row.get("companyCode")));
        info.setIndustryName(StringUtils.hasText(asString(row.get("industryName"))) ? asString(row.get("industryName")) : industryName);
        info.setCompanyType(asString(row.get("companyType")));
        info.setDataYear(asInteger(row.get("dataYear")));
        info.setInstitutionName(institutionName);
        info.setTransitionScore(asBigDecimal(row.get("transitionScore")).setScale(2, RoundingMode.HALF_UP));
        return info;
    }

    private Map<String, BigDecimal> resolveFeatureWeights(String institutionCode, String industryKey, String modelMode) {
        String weightsJson;
        if ("federated".equals(modelMode)) {
            FederatedModelVO model = federatedLearningMapper.selectReadyFederatedModel(industryKey);
            if (model == null) {
                throw new BusinessException("当前行业联邦模型尚未训练完成，无法生成报告");
            }
            weightsJson = model.getFeatureWeightsJson();
        } else {
            IndustryModelVO model = modelTrainingMapper.selectReadyIndustryModel(institutionCode, industryKey);
            if (model == null) {
                throw new BusinessException("当前金融机构行业模型尚未训练完成，无法生成报告");
            }
            weightsJson = model.getFeatureWeightsJson();
        }
        Map<String, BigDecimal> weights = parseWeights(weightsJson);
        if (weights.isEmpty()) {
            throw new BusinessException("模型权重为空，无法生成报告");
        }
        return weights;
    }

    private Map<String, BigDecimal> parseWeights(String weightsJson) {
        if (!StringUtils.hasText(weightsJson)) {
            return Map.of();
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(weightsJson, new TypeReference<>() {
            });
            Map<String, BigDecimal> weights = new HashMap<>();
            for (Map<String, Object> row : rows) {
                Object prop = row.get("prop");
                Object weight = row.get("weight");
                if (prop != null && weight != null) {
                    weights.put(prop.toString(), asBigDecimal(weight));
                }
            }
            return weights;
        } catch (Exception exception) {
            throw new BusinessException("模型权重解析失败，无法生成报告");
        }
    }

    private List<AssessmentReportVO.IndicatorItem> buildIndicatorItems(String industryKey,
                                                                       Map<String, Object> row,
                                                                       List<IndustryDataCatalog.FieldMeta> fields,
                                                                       Map<String, BigDecimal> weights,
                                                                       boolean common) {
        return fields.stream()
                .filter(field -> common == COMMON_PROPS.contains(field.prop()))
                .map(field -> buildIndicatorItem(industryKey, row, field, weights.getOrDefault(field.prop(), BigDecimal.ZERO)))
                .sorted(Comparator.comparing(AssessmentReportVO.IndicatorItem::getContribution).reversed())
                .collect(Collectors.toList());
    }

    private AssessmentReportVO.IndicatorItem buildIndicatorItem(String industryKey,
                                                                Map<String, Object> row,
                                                                IndustryDataCatalog.FieldMeta field,
                                                                BigDecimal weight) {
        BigDecimal normalizedScore = normalizeScore(industryKey, field, row.get(field.prop()));
        AssessmentReportVO.IndicatorItem item = new AssessmentReportVO.IndicatorItem();
        item.setProp(field.prop());
        item.setLabel(field.label());
        item.setRawValue(row.get(field.prop()));
        item.setNormalizedScore(normalizedScore);
        item.setWeight(weight.setScale(4, RoundingMode.HALF_UP));
        item.setContribution(normalizedScore.multiply(weight).setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    private BigDecimal normalizeScore(String industryKey, IndustryDataCatalog.FieldMeta field, Object rawValue) {
        if (rawValue == null || !StringUtils.hasText(rawValue.toString())) {
            return BigDecimal.ZERO;
        }
        if ("boolean".equals(field.type())) {
            return normalizeBoolean(rawValue);
        }
        if ("environmentalPerformanceRating".equals(field.prop())) {
            return normalizeRating(rawValue);
        }
        IndicatorScale scale = SCALES.getOrDefault(industryKey + ":" + field.prop(), SCALES.get(field.prop()));
        if (scale == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = asBigDecimal(rawValue);
        BigDecimal numerator = "lower".equals(scale.direction())
                ? scale.lag().subtract(value)
                : value.subtract(scale.lag());
        BigDecimal denominator = "lower".equals(scale.direction())
                ? scale.lag().subtract(scale.best())
                : scale.best().subtract(scale.lag());
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return clamp(numerator.divide(denominator, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
    }

    private BigDecimal normalizeBoolean(Object rawValue) {
        if (rawValue instanceof Boolean boolValue) {
            return boolValue ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        String text = rawValue.toString().trim();
        return "1".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text)
                ? BigDecimal.valueOf(100)
                : BigDecimal.ZERO;
    }

    private BigDecimal normalizeRating(Object rawValue) {
        String text = rawValue.toString().trim().toUpperCase();
        if (text.contains("A")) {
            return BigDecimal.valueOf(100);
        }
        if (text.contains("B")) {
            return BigDecimal.valueOf(70);
        }
        if (text.contains("C")) {
            return BigDecimal.valueOf(35);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private FinancialInstitution requireInstitution(String institutionId) {
        FinancialInstitution institution = institutionMapper.selectById(institutionId);
        if (institution == null) {
            throw new BusinessException("金融机构不存在，请重新登录");
        }
        return institution;
    }

    private String buildReportNo(String industryKey, Long id, Object dataYear) {
        String yearText = dataYear == null ? "NA" : dataYear.toString();
        return "TF-" + yearText + "-" + industryKey.toUpperCase() + "-" + id;
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString().trim());
    }

    private static Map<String, IndicatorScale> createScales() {
        Map<String, IndicatorScale> scales = new LinkedHashMap<>();
        putScale(scales, "salesNetProfitMargin", 20, 0, "higher");
        putScale(scales, "revenueGrowthRate", 30, -10, "higher");
        putScale(scales, "rAndDIntensity", 10, 0, "higher");
        putScale(scales, "electrolyticAluminumRenewablePowerRatio", 50, 5, "higher");
        putScale(scales, "electrolyticAluminumDcPowerConsumption", 12500, 13500, "lower");
        putScale(scales, "recycledMetalSupplyRatio", 40, 3, "higher");
        putScale(scales, "recycledAluminumOutput", 80, 0.5, "higher");
        putScale(scales, "aluminumCarbonEmissionIntensity", 2, 13, "lower");
        putScale(scales, "moltenAluminumDirectAlloyingRatio", 95, 70, "higher");
        putScale(scales, "cleanEnergyInstalledCapacityRatio", 90, 5, "higher");
        putScale(scales, "cleanEnergyGenerationRatio", 95, 5, "higher");
        putScale(scales, "powerSupplyStandardCoalConsumption", 270, 320, "lower");
        putScale(scales, "powerGenerationCarbonIntensity", 0.05, 1, "lower");
        putScale(scales, "coalPowerFlexibilityRetrofitRatio", 80, 5, "higher");
        putScale(scales, "minimumTechnicalOutputPeakDepth", 20, 50, "lower");
        putScale(scales, "renewableEnergyInvestment", 200, 0.5, "higher");
        putScale(scales, "blastFurnaceEnergyConsumption", 361, 460, "lower");
        putScale(scales, "converterEnergyConsumption", -30, 5, "lower");
        putScale(scales, "electricArcFurnaceEnergyConsumption", 62, 95, "lower");
        putScale(scales, "steelCarbonEmissionIntensity", 0.6, 2.3, "lower");
        putScale(scales, "electricFurnaceShortProcessRatio", 40, 3, "higher");
        putScale(scales, "scrapSteelUse", 450, 60, "higher");
        putScale(scales, "byproductGasUtilizationRatio", 99.5, 90, "higher");
        putScale(scales, "aviationFuelConsumptionPerTonKm", 0.28, 0.34, "lower");
        putScale(scales, "aviationCo2EmissionPerTonKm", 0.85, 1, "lower");
        putScale(scales, "co2EmissionPerRpk", 78, 95, "lower");
        putScale(scales, "safUsage", 3000, 0, "higher");
        putScale(scales, "passengerThroughputEnergyConsumption", 0.8, 1, "lower");
        putScale(scales, "passengerThroughputCo2Emission", 0.4, 0.55, "lower");
        putScale(scales, "renewableEnergyConsumptionRatio", 15, 2, "higher");
        putScale(scales, "groundVehicleElectrificationRatio", 70, 10, "higher");
        putScale(scales, "syntheticAmmoniaEnergyConsumption", 1350, 1650, "lower");
        putScale(scales, "ethyleneNaphthaCrackingEnergyConsumption", 590, 680, "lower");
        putScale(scales, "methanolEnergyConsumption", 1250, 1600, "lower");
        putScale(scales, "productCarbonEmissionIntensity", 1.5, 4, "lower");
        putScale(scales, "greenPowerUseRatio", 50, 3, "higher");
        putScale(scales, "greenHydrogenBiomassNonFossilFeedstockRatio", 20, 0, "higher");
        putScale(scales, "industrialWaterReuseRatio", 99, 92, "higher");
        putScale(scales, "clinkerEnergyConsumption", 1, 125, "lower");
        putScale(scales, "flatGlassEnergyConsumption", 8.5, 14, "lower");
        putIndustryScale(scales, "building", "productCarbonEmissionIntensity", 0.6, 0.9, "lower");
        putScale(scales, "alternativeFuelThermalSubstitutionRatio", 20, 1, "higher");
        putScale(scales, "alternativeRawMaterialUseRatio", 30, 2, "higher");
        putScale(scales, "coalMineGasExtractionUtilizationRatio", 65, 20, "higher");
        putScale(scales, "rawCoalEnergyConsumption", 3, 8, "lower");
        putScale(scales, "mineWaterReuseRatio", 95, 55, "higher");
        putScale(scales, "gangueFlyAshReuseRatio", 90, 55, "higher");
        putScale(scales, "nonCoalRevenueRatio", 40, 2, "higher");
        putScale(scales, "newEnergyInstalledCapacity", 3000, 0, "higher");
        putScale(scales, "refiningEnergyConsumptionByEnergyFactor", 7, 9, "lower");
        putScale(scales, "refiningCarbonEmissionIntensity", 0.15, 0.35, "lower");
        putScale(scales, "methaneEmissionIntensity", 0.5, 3, "lower");
        putScale(scales, "chemicalProductYieldRatio", 55, 25, "higher");
        putScale(scales, "cleanEnergyUseRatio", 20, 2, "higher");
        putScale(scales, "sustainableAviationFuelBiofuelOutputSales", 50, 0, "higher");
        return Map.copyOf(scales);
    }

    private static void putScale(Map<String, IndicatorScale> scales,
                                 String prop,
                                 double best,
                                 double lag,
                                 String direction) {
        scales.put(prop, new IndicatorScale(BigDecimal.valueOf(best), BigDecimal.valueOf(lag), direction));
    }

    private static void putIndustryScale(Map<String, IndicatorScale> scales,
                                         String industryKey,
                                         String prop,
                                         double best,
                                         double lag,
                                         String direction) {
        scales.put(industryKey + ":" + prop, new IndicatorScale(BigDecimal.valueOf(best), BigDecimal.valueOf(lag), direction));
    }

    /**
     * 指标标准化区间定义。
     */
    private record IndicatorScale(BigDecimal best, BigDecimal lag, String direction) {
    }
}
