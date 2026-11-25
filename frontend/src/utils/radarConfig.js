// 雷达图指标配置来自《转型金融评分模拟数据2.0_指标字典.xlsx》。
export const comprehensiveIndicators = [
  { prop: 'transitionTargetCommitment', label: '转型目标与承诺', type: 'boolean' },
  { prop: 'transitionStrategyPath', label: '转型战略与路径规划', type: 'boolean' },
  { prop: 'governanceIncentive', label: '治理架构与激励', type: 'boolean' },
  { prop: 'transitionTechnologyApplication', label: '转型技术应用现状', type: 'boolean' },
  { prop: 'carbonAssetManagement', label: '碳资产管理能力', type: 'boolean' },
  { prop: 'riskManagementMarketOpportunity', label: '风险管理与市场机遇', type: 'boolean' },
  { prop: 'informationDisclosureTransparency', label: '信息披露与透明度', type: 'boolean' },
  { prop: 'justTransition', label: '公正转型', type: 'boolean' },
  { prop: 'environmentalCompliance', label: '环境合规', type: 'boolean' },
  { prop: 'salesNetProfitMargin', label: '销售净利率', type: 'range', best: 20, lag: 0, direction: 'higher' },
  { prop: 'revenueGrowthRate', label: '营业收入增长率', type: 'range', best: 30, lag: -10, direction: 'higher' },
  { prop: 'rAndDIntensity', label: '研发投入强度', type: 'range', best: 10, lag: 0, direction: 'higher' }
]

export const specialtyIndicators = {
  steel: [
    { prop: 'blastFurnaceEnergyConsumption', label: '高炉工序能耗', best: 361, lag: 460, direction: 'lower' },
    { prop: 'converterEnergyConsumption', label: '转炉工序能耗', best: -30, lag: 5, direction: 'lower' },
    { prop: 'electricArcFurnaceEnergyConsumption', label: '电弧炉炼钢能耗', best: 62, lag: 95, direction: 'lower' },
    { prop: 'steelCarbonEmissionIntensity', label: '吨钢碳排放强度', best: 0.6, lag: 2.3, direction: 'lower' },
    { prop: 'electricFurnaceShortProcessRatio', label: '电炉短流程占比', best: 40, lag: 3, direction: 'higher' },
    { prop: 'scrapSteelUse', label: '废钢利用量', best: 450, lag: 60, direction: 'higher' },
    { prop: 'byproductGasUtilizationRatio', label: '煤气利用率', best: 99.5, lag: 90, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  power: [
    { prop: 'cleanEnergyInstalledCapacityRatio', label: '清洁能源装机占比', best: 90, lag: 5, direction: 'higher' },
    { prop: 'cleanEnergyGenerationRatio', label: '清洁能源发电占比', best: 95, lag: 5, direction: 'higher' },
    { prop: 'powerSupplyStandardCoalConsumption', label: '供电标准煤耗', best: 270, lag: 320, direction: 'lower' },
    { prop: 'powerGenerationCarbonIntensity', label: '发电碳排放强度', best: 0.05, lag: 1, direction: 'lower' },
    { prop: 'coalPowerFlexibilityRetrofitRatio', label: '煤电灵活性改造比例', best: 80, lag: 5, direction: 'higher' },
    { prop: 'minimumTechnicalOutputPeakDepth', label: '最小技术出力调峰深度', best: 20, lag: 50, direction: 'lower' },
    { prop: 'renewableEnergyInvestment', label: '可再生能源投资额', best: 200, lag: 0.5, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  chemical: [
    { prop: 'syntheticAmmoniaEnergyConsumption', label: '合成氨综合能耗', best: 1350, lag: 1650, direction: 'lower' },
    { prop: 'ethyleneNaphthaCrackingEnergyConsumption', label: '乙烯裂解综合能耗', best: 590, lag: 680, direction: 'lower' },
    { prop: 'methanolEnergyConsumption', label: '甲醇综合能耗', best: 1250, lag: 1600, direction: 'lower' },
    { prop: 'productCarbonEmissionIntensity', label: '产品碳排放强度', best: 1.5, lag: 4, direction: 'lower' },
    { prop: 'greenPowerUseRatio', label: '绿色电力使用比例', best: 50, lag: 3, direction: 'higher' },
    { prop: 'greenHydrogenBiomassNonFossilFeedstockRatio', label: '非化石原料使用比例', best: 20, lag: 0, direction: 'higher' },
    { prop: 'industrialWaterReuseRatio', label: '工业水重复利用率', best: 99, lag: 92, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  petrochemical: [
    { prop: 'refiningEnergyConsumptionByEnergyFactor', label: '炼油综合能耗', best: 7, lag: 9, direction: 'lower' },
    { prop: 'refiningCarbonEmissionIntensity', label: '炼油碳排放强度', best: 0.15, lag: 0.35, direction: 'lower' },
    { prop: 'methaneEmissionIntensity', label: '甲烷排放强度', best: 0.5, lag: 3, direction: 'lower' },
    { prop: 'chemicalProductYieldRatio', label: '化工品收率占比', best: 55, lag: 25, direction: 'higher' },
    { prop: 'cleanEnergyUseRatio', label: '清洁能源使用比例', best: 20, lag: 2, direction: 'higher' },
    { prop: 'sustainableAviationFuelBiofuelOutputSales', label: '可持续航空燃料产销量', best: 50, lag: 0, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  building: [
    { prop: 'clinkerEnergyConsumption', label: '水泥熟料综合能耗', best: 1, lag: 125, direction: 'lower' },
    { prop: 'flatGlassEnergyConsumption', label: '平板玻璃综合能耗', best: 8.5, lag: 14, direction: 'lower' },
    { prop: 'productCarbonEmissionIntensity', label: '产品碳排放强度', best: 0.6, lag: 0.9, direction: 'lower' },
    { prop: 'alternativeFuelThermalSubstitutionRatio', label: '替代燃料热替代率', best: 20, lag: 1, direction: 'higher' },
    { prop: 'alternativeRawMaterialUseRatio', label: '替代原料使用比例', best: 30, lag: 2, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  nonferrous: [
    { prop: 'electrolyticAluminumRenewablePowerRatio', label: '电解铝绿电比例', best: 50, lag: 5, direction: 'higher' },
    { prop: 'electrolyticAluminumDcPowerConsumption', label: '电解铝直流电耗', best: 12500, lag: 13500, direction: 'lower' },
    { prop: 'recycledMetalSupplyRatio', label: '再生金属供应占比', best: 40, lag: 3, direction: 'higher' },
    { prop: 'recycledAluminumOutput', label: '再生铝产量', best: 80, lag: 0.5, direction: 'higher' },
    { prop: 'aluminumCarbonEmissionIntensity', label: '吨铝碳排放强度', best: 2, lag: 13, direction: 'lower' },
    { prop: 'moltenAluminumDirectAlloyingRatio', label: '铝水直接合金化比例', best: 95, lag: 70, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  coal: [
    { prop: 'coalMineGasExtractionUtilizationRatio', label: '瓦斯抽采利用率', best: 65, lag: 20, direction: 'higher' },
    { prop: 'rawCoalEnergyConsumption', label: '原煤综合能耗', best: 3, lag: 8, direction: 'lower' },
    { prop: 'mineWaterReuseRatio', label: '矿井水利用率', best: 95, lag: 55, direction: 'higher' },
    { prop: 'gangueFlyAshReuseRatio', label: '固废综合利用率', best: 90, lag: 55, direction: 'higher' },
    { prop: 'nonCoalRevenueRatio', label: '非煤收入占比', best: 40, lag: 2, direction: 'higher' },
    { prop: 'newEnergyInstalledCapacity', label: '新能源装机容量', best: 3000, lag: 0, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ],
  aviation: [
    { prop: 'aviationFuelConsumptionPerTonKm', label: '吨公里油耗', best: 0.28, lag: 0.34, direction: 'lower' },
    { prop: 'aviationCo2EmissionPerTonKm', label: '吨公里CO2排放', best: 0.85, lag: 1, direction: 'lower' },
    { prop: 'co2EmissionPerRpk', label: 'RPK CO2排放', best: 78, lag: 95, direction: 'lower' },
    { prop: 'safUsage', label: 'SAF使用量', best: 3000, lag: 0, direction: 'higher' },
    { prop: 'passengerThroughputEnergyConsumption', label: '旅客吞吐量能耗', best: 0.8, lag: 1, direction: 'lower' },
    { prop: 'passengerThroughputCo2Emission', label: '旅客吞吐量CO2排放', best: 0.4, lag: 0.55, direction: 'lower' },
    { prop: 'renewableEnergyConsumptionRatio', label: '可再生能源消费占比', best: 15, lag: 2, direction: 'higher' },
    { prop: 'groundVehicleElectrificationRatio', label: '地面车辆电动化比例', best: 70, lag: 10, direction: 'higher' },
    { prop: 'environmentalPerformanceRating', label: '环保绩效评级', type: 'rating' }
  ]
}
