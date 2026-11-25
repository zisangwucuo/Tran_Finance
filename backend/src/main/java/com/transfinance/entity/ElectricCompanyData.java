package com.transfinance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 电力行业企业历年转型评估数据实体。
 */
@TableName("electric_company_data")
public class ElectricCompanyData {

    /** 数据ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 行业代码。 */
    private String industryCode;

    /** 股票代码。 */
    private String stockCode;

    /** 公司名称。 */
    private String companyName;

    /** 数据年份。 */
    private Integer dataYear;

    /** 企业年份键。 */
    private String companyYearKey;

    /** 是否纳入碳市场。 */
    private Integer carbonMarketIncluded;

    /** 是否获得地方转型金融政策支持。 */
    private Integer localTransitionPolicySupport;

    /** 是否有双碳转型承诺目标。 */
    private Integer doubleCarbonCommitment;

    /** 是否已编制转型计划。 */
    private Integer transitionPlanPrepared;

    /** 绿色专利授权数量占比。 */
    private BigDecimal greenPatentRatio;

    /** 节能环保投资强度。 */
    private BigDecimal energyEnvironmentInvestmentIntensity;

    /** 非两高业务收入占比。 */
    private BigDecimal nonTwoHighRevenueRatio;

    /** 数字技术应用水平。 */
    private BigDecimal digitalTechnologyLevel;

    /** 高管薪酬是否与 ESG 挂钩。 */
    private Integer executiveCompensationEsgLinked;

    /** 是否经过第三方核查。 */
    private Integer thirdPartyVerification;

    /** 信息披露质量。 */
    private BigDecimal disclosureQuality;

    /** 供电煤耗。 */
    private BigDecimal powerSupplyCoalConsumption;

    /** 清洁能源装机占比。 */
    private BigDecimal cleanEnergyInstalledCapacityRatio;

    /** 度电碳排放强度。 */
    private BigDecimal powerCarbonIntensity;

    /** CCER 开发量。 */
    private BigDecimal ccerDevelopmentVolume;

    /** 流动比率。 */
    private BigDecimal currentRatio;

    /** 营收增长率。 */
    private BigDecimal revenueGrowthRate;

    /** 现金流比率。 */
    private BigDecimal cashFlowRatio;

    /** 资产负债率。 */
    private BigDecimal assetLiabilityRatio;

    /** 总资产收益率。 */
    private BigDecimal roa;

    /** 环保处罚次数。 */
    private BigDecimal environmentalPenaltyCount;

    /** 行业景气指数。 */
    private BigDecimal industryProsperityIndex;

    /** 碳价。 */
    private BigDecimal carbonPrice;

    /** 目标变量：是否获得贷款。 */
    private Integer loanGranted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Integer getDataYear() {
        return dataYear;
    }

    public void setDataYear(Integer dataYear) {
        this.dataYear = dataYear;
    }

    public String getCompanyYearKey() {
        return companyYearKey;
    }

    public void setCompanyYearKey(String companyYearKey) {
        this.companyYearKey = companyYearKey;
    }

    public Integer getCarbonMarketIncluded() {
        return carbonMarketIncluded;
    }

    public void setCarbonMarketIncluded(Integer carbonMarketIncluded) {
        this.carbonMarketIncluded = carbonMarketIncluded;
    }

    public Integer getLocalTransitionPolicySupport() {
        return localTransitionPolicySupport;
    }

    public void setLocalTransitionPolicySupport(Integer localTransitionPolicySupport) {
        this.localTransitionPolicySupport = localTransitionPolicySupport;
    }

    public Integer getDoubleCarbonCommitment() {
        return doubleCarbonCommitment;
    }

    public void setDoubleCarbonCommitment(Integer doubleCarbonCommitment) {
        this.doubleCarbonCommitment = doubleCarbonCommitment;
    }

    public Integer getTransitionPlanPrepared() {
        return transitionPlanPrepared;
    }

    public void setTransitionPlanPrepared(Integer transitionPlanPrepared) {
        this.transitionPlanPrepared = transitionPlanPrepared;
    }

    public BigDecimal getGreenPatentRatio() {
        return greenPatentRatio;
    }

    public void setGreenPatentRatio(BigDecimal greenPatentRatio) {
        this.greenPatentRatio = greenPatentRatio;
    }

    public BigDecimal getEnergyEnvironmentInvestmentIntensity() {
        return energyEnvironmentInvestmentIntensity;
    }

    public void setEnergyEnvironmentInvestmentIntensity(BigDecimal energyEnvironmentInvestmentIntensity) {
        this.energyEnvironmentInvestmentIntensity = energyEnvironmentInvestmentIntensity;
    }

    public BigDecimal getNonTwoHighRevenueRatio() {
        return nonTwoHighRevenueRatio;
    }

    public void setNonTwoHighRevenueRatio(BigDecimal nonTwoHighRevenueRatio) {
        this.nonTwoHighRevenueRatio = nonTwoHighRevenueRatio;
    }

    public BigDecimal getDigitalTechnologyLevel() {
        return digitalTechnologyLevel;
    }

    public void setDigitalTechnologyLevel(BigDecimal digitalTechnologyLevel) {
        this.digitalTechnologyLevel = digitalTechnologyLevel;
    }

    public Integer getExecutiveCompensationEsgLinked() {
        return executiveCompensationEsgLinked;
    }

    public void setExecutiveCompensationEsgLinked(Integer executiveCompensationEsgLinked) {
        this.executiveCompensationEsgLinked = executiveCompensationEsgLinked;
    }

    public Integer getThirdPartyVerification() {
        return thirdPartyVerification;
    }

    public void setThirdPartyVerification(Integer thirdPartyVerification) {
        this.thirdPartyVerification = thirdPartyVerification;
    }

    public BigDecimal getDisclosureQuality() {
        return disclosureQuality;
    }

    public void setDisclosureQuality(BigDecimal disclosureQuality) {
        this.disclosureQuality = disclosureQuality;
    }

    public BigDecimal getPowerSupplyCoalConsumption() {
        return powerSupplyCoalConsumption;
    }

    public void setPowerSupplyCoalConsumption(BigDecimal powerSupplyCoalConsumption) {
        this.powerSupplyCoalConsumption = powerSupplyCoalConsumption;
    }

    public BigDecimal getCleanEnergyInstalledCapacityRatio() {
        return cleanEnergyInstalledCapacityRatio;
    }

    public void setCleanEnergyInstalledCapacityRatio(BigDecimal cleanEnergyInstalledCapacityRatio) {
        this.cleanEnergyInstalledCapacityRatio = cleanEnergyInstalledCapacityRatio;
    }

    public BigDecimal getPowerCarbonIntensity() {
        return powerCarbonIntensity;
    }

    public void setPowerCarbonIntensity(BigDecimal powerCarbonIntensity) {
        this.powerCarbonIntensity = powerCarbonIntensity;
    }

    public BigDecimal getCcerDevelopmentVolume() {
        return ccerDevelopmentVolume;
    }

    public void setCcerDevelopmentVolume(BigDecimal ccerDevelopmentVolume) {
        this.ccerDevelopmentVolume = ccerDevelopmentVolume;
    }

    public BigDecimal getCurrentRatio() {
        return currentRatio;
    }

    public void setCurrentRatio(BigDecimal currentRatio) {
        this.currentRatio = currentRatio;
    }

    public BigDecimal getRevenueGrowthRate() {
        return revenueGrowthRate;
    }

    public void setRevenueGrowthRate(BigDecimal revenueGrowthRate) {
        this.revenueGrowthRate = revenueGrowthRate;
    }

    public BigDecimal getCashFlowRatio() {
        return cashFlowRatio;
    }

    public void setCashFlowRatio(BigDecimal cashFlowRatio) {
        this.cashFlowRatio = cashFlowRatio;
    }

    public BigDecimal getAssetLiabilityRatio() {
        return assetLiabilityRatio;
    }

    public void setAssetLiabilityRatio(BigDecimal assetLiabilityRatio) {
        this.assetLiabilityRatio = assetLiabilityRatio;
    }

    public BigDecimal getRoa() {
        return roa;
    }

    public void setRoa(BigDecimal roa) {
        this.roa = roa;
    }

    public BigDecimal getEnvironmentalPenaltyCount() {
        return environmentalPenaltyCount;
    }

    public void setEnvironmentalPenaltyCount(BigDecimal environmentalPenaltyCount) {
        this.environmentalPenaltyCount = environmentalPenaltyCount;
    }

    public BigDecimal getIndustryProsperityIndex() {
        return industryProsperityIndex;
    }

    public void setIndustryProsperityIndex(BigDecimal industryProsperityIndex) {
        this.industryProsperityIndex = industryProsperityIndex;
    }

    public BigDecimal getCarbonPrice() {
        return carbonPrice;
    }

    public void setCarbonPrice(BigDecimal carbonPrice) {
        this.carbonPrice = carbonPrice;
    }

    public Integer getLoanGranted() {
        return loanGranted;
    }

    public void setLoanGranted(Integer loanGranted) {
        this.loanGranted = loanGranted;
    }
}
