package com.transfinance.support;

import com.transfinance.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 八个行业数据表与模型特征字段目录。
 * 训练前缺失检查、企业端补全、金融机构端通知共用这份白名单，避免动态表名和字段名被外部输入控制。
 */
public final class IndustryDataCatalog {

    private static final Map<String, IndustryMeta> INDUSTRIES = createIndustries();

    private IndustryDataCatalog() {
    }

    public static Map<String, IndustryMeta> industries() {
        return INDUSTRIES;
    }

    public static IndustryMeta requireIndustry(String industryKey) {
        IndustryMeta industry = INDUSTRIES.get(industryKey);
        if (industry == null) {
            throw new BusinessException("行业不存在");
        }
        return industry;
    }

    public static FieldMeta requireField(String industryKey, String prop) {
        return requireIndustry(industryKey).featureFields().stream()
                .filter(field -> field.prop().equals(prop))
                .findFirst()
                .orElseThrow(() -> new BusinessException("字段不存在或不允许补全"));
    }

    private static Map<String, IndustryMeta> createIndustries() {
        Map<String, IndustryMeta> industries = new LinkedHashMap<>();
        addIndustry(industries, "nonferrous", "有色金属行业", "nonferrous_metal_company_data", List.of(
                number("electrolyticAluminumRenewablePowerRatio", "electrolytic_aluminum_renewable_power_ratio", "电解铝绿电比例(%)"),
                number("electrolyticAluminumDcPowerConsumption", "electrolytic_aluminum_dc_power_consumption", "电解铝直流电耗"),
                number("recycledMetalSupplyRatio", "recycled_metal_supply_ratio", "再生金属供应占比(%)"),
                number("recycledAluminumOutput", "recycled_aluminum_output", "再生铝产量(万吨)"),
                number("aluminumCarbonEmissionIntensity", "aluminum_carbon_emission_intensity", "吨铝碳排放强度"),
                number("moltenAluminumDirectAlloyingRatio", "molten_aluminum_direct_alloying_ratio", "铝水直接合金化比例(%)")
        ));
        addIndustry(industries, "power", "电力行业", "power_company_data", List.of(
                number("cleanEnergyInstalledCapacityRatio", "clean_energy_installed_capacity_ratio", "清洁能源装机占比(%)"),
                number("cleanEnergyGenerationRatio", "clean_energy_generation_ratio", "清洁能源发电量占比(%)"),
                number("powerSupplyStandardCoalConsumption", "power_supply_standard_coal_consumption", "供电标准煤耗"),
                number("powerGenerationCarbonIntensity", "power_generation_carbon_intensity", "发电碳排放强度"),
                number("coalPowerFlexibilityRetrofitRatio", "coal_power_flexibility_retrofit_ratio", "煤电灵活性改造比例(%)"),
                number("minimumTechnicalOutputPeakDepth", "minimum_technical_output_peak_depth", "最小技术出力调峰深度(%)"),
                number("renewableEnergyInvestment", "renewable_energy_investment", "可再生能源投资额(亿元)")
        ));
        addIndustry(industries, "steel", "钢铁行业", "steel_company_data", List.of(
                number("blastFurnaceEnergyConsumption", "blast_furnace_energy_consumption", "高炉工序能耗"),
                number("converterEnergyConsumption", "converter_energy_consumption", "转炉工序能耗"),
                number("electricArcFurnaceEnergyConsumption", "electric_arc_furnace_energy_consumption", "电弧炉炼钢能耗"),
                number("steelCarbonEmissionIntensity", "steel_carbon_emission_intensity", "吨钢碳排放强度"),
                number("electricFurnaceShortProcessRatio", "electric_furnace_short_process_ratio", "电炉短流程占比(%)"),
                number("scrapSteelUse", "scrap_steel_use", "废钢利用量"),
                number("byproductGasUtilizationRatio", "byproduct_gas_utilization_ratio", "煤气利用率(%)")
        ));
        addIndustry(industries, "aviation", "航空机场行业", "aviation_airport_company_data", List.of(
                text("subIndustryType", "sub_industry_type", "细分类型"),
                number("aviationFuelConsumptionPerTonKm", "aviation_fuel_consumption_per_ton_km", "吨公里油耗"),
                number("aviationCo2EmissionPerTonKm", "aviation_co2_emission_per_ton_km", "吨公里CO2排放"),
                number("co2EmissionPerRpk", "co2_emission_per_rpk", "RPK碳排放"),
                number("safUsage", "saf_usage", "SAF使用量(t)"),
                number("passengerThroughputEnergyConsumption", "passenger_throughput_energy_consumption", "旅客吞吐量能耗"),
                number("passengerThroughputCo2Emission", "passenger_throughput_co2_emission", "旅客吞吐量CO2排放"),
                number("renewableEnergyConsumptionRatio", "renewable_energy_consumption_ratio", "可再生能源消费占比(%)"),
                number("groundVehicleElectrificationRatio", "ground_vehicle_electrification_ratio", "地面车辆电动化比例(%)")
        ));
        addIndustry(industries, "chemical", "基础化工行业", "basic_chemical_company_data", List.of(
                number("syntheticAmmoniaEnergyConsumption", "synthetic_ammonia_energy_consumption", "合成氨综合能耗"),
                number("ethyleneNaphthaCrackingEnergyConsumption", "ethylene_naphtha_cracking_energy_consumption", "乙烯裂解综合能耗"),
                number("methanolEnergyConsumption", "methanol_energy_consumption", "甲醇综合能耗"),
                number("productCarbonEmissionIntensity", "product_carbon_emission_intensity", "产品碳排放强度"),
                number("greenPowerUseRatio", "green_power_use_ratio", "绿色电力使用比例(%)"),
                number("greenHydrogenBiomassNonFossilFeedstockRatio", "green_hydrogen_biomass_non_fossil_feedstock_ratio", "非化石原料使用比例(%)"),
                number("industrialWaterReuseRatio", "industrial_water_reuse_ratio", "工业水重复利用率(%)")
        ));
        addIndustry(industries, "building", "建材行业", "building_material_company_data", List.of(
                number("clinkerEnergyConsumption", "clinker_energy_consumption", "水泥熟料综合能耗"),
                number("flatGlassEnergyConsumption", "flat_glass_energy_consumption", "平板玻璃综合能耗"),
                number("productCarbonEmissionIntensity", "product_carbon_emission_intensity", "产品碳排放强度"),
                number("alternativeFuelThermalSubstitutionRatio", "alternative_fuel_thermal_substitution_ratio", "替代燃料热量替代率(%)"),
                number("alternativeRawMaterialUseRatio", "alternative_raw_material_use_ratio", "替代原料使用比例(%)")
        ));
        addIndustry(industries, "coal", "煤炭行业", "coal_company_data", List.of(
                number("coalMineGasExtractionUtilizationRatio", "coal_mine_gas_extraction_utilization_ratio", "瓦斯抽采利用率(%)"),
                number("rawCoalEnergyConsumption", "raw_coal_energy_consumption", "原煤综合能耗"),
                number("mineWaterReuseRatio", "mine_water_reuse_ratio", "矿井水利用率(%)"),
                number("gangueFlyAshReuseRatio", "gangue_fly_ash_reuse_ratio", "煤矸石粉煤灰利用率(%)"),
                number("nonCoalRevenueRatio", "non_coal_revenue_ratio", "非煤收入占比(%)"),
                number("newEnergyInstalledCapacity", "new_energy_installed_capacity", "新能源装机容量(MW)")
        ));
        addIndustry(industries, "petrochemical", "石油石化行业", "petrochemical_company_data", List.of(
                number("refiningEnergyConsumptionByEnergyFactor", "refining_energy_consumption_by_energy_factor", "炼油综合能耗"),
                number("refiningCarbonEmissionIntensity", "refining_carbon_emission_intensity", "炼油碳排放强度"),
                number("methaneEmissionIntensity", "methane_emission_intensity", "甲烷排放强度"),
                number("chemicalProductYieldRatio", "chemical_product_yield_ratio", "化工品收率占比(%)"),
                number("cleanEnergyUseRatio", "clean_energy_use_ratio", "清洁能源使用比例(%)"),
                number("sustainableAviationFuelBiofuelOutputSales", "sustainable_aviation_fuel_biofuel_output_sales", "可持续航空燃料产销量")
        ));
        return Map.copyOf(industries);
    }

    private static void addIndustry(Map<String, IndustryMeta> industries,
                                    String key,
                                    String name,
                                    String tableName,
                                    List<FieldMeta> specialFields) {
        List<FieldMeta> fields = new ArrayList<>();
        fields.add(bool("transitionTargetCommitment", "transition_target_commitment", "转型目标与承诺"));
        fields.add(bool("transitionStrategyPath", "transition_strategy_path", "转型战略与路径规划"));
        fields.add(bool("governanceIncentive", "governance_incentive", "治理架构与激励"));
        fields.add(bool("transitionTechnologyApplication", "transition_technology_application", "转型技术应用现状"));
        fields.add(bool("carbonAssetManagement", "carbon_asset_management", "碳资产管理能力"));
        fields.add(bool("riskManagementMarketOpportunity", "risk_management_market_opportunity", "风险管理与市场机遇"));
        fields.add(bool("informationDisclosureTransparency", "information_disclosure_transparency", "信息披露与透明度"));
        fields.add(bool("justTransition", "just_transition", "公正转型"));
        fields.add(bool("environmentalCompliance", "environmental_compliance", "环境合规"));
        fields.add(number("salesNetProfitMargin", "sales_net_profit_margin", "销售净利率(%)"));
        fields.add(number("revenueGrowthRate", "revenue_growth_rate", "营业收入增长率(%)"));
        fields.add(number("rAndDIntensity", "r_and_d_intensity", "研发投入强度(%)"));
        fields.addAll(specialFields);
        fields.add(text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级"));
        industries.put(key, new IndustryMeta(key, name, tableName, fields));
    }

    private static FieldMeta text(String prop, String column, String label) {
        return new FieldMeta(prop, column, label, "text");
    }

    private static FieldMeta number(String prop, String column, String label) {
        return new FieldMeta(prop, column, label, "number");
    }

    private static FieldMeta bool(String prop, String column, String label) {
        return new FieldMeta(prop, column, label, "boolean");
    }

    public record IndustryMeta(String key, String name, String tableName, List<FieldMeta> featureFields) {

        public String pendingTableName() {
            return tableName.replace("_company_data", "_pending_company_data");
        }
    }

    public record FieldMeta(String prop, String column, String label, String type) {
    }
}
