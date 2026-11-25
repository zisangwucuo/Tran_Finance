package com.transfinance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 企业评估数据新增/修改请求。
 */
public class ElectricCompanyDataSaveRequest {

    /** 行业代码，首版默认 D44。 */
    @NotBlank(message = "不能为空")
    private String industryCode = "D44";

    /** 股票代码。 */
    @NotBlank(message = "不能为空")
    private String stockCode;

    /** 公司名称。 */
    @NotBlank(message = "不能为空")
    private String companyName;

    /** 数据年份。 */
    @NotNull(message = "不能为空")
    @Min(value = 2000, message = "不能早于2000年")
    @Max(value = 2100, message = "不能晚于2100年")
    private Integer dataYear;

    /** 企业年份键。 */
    @NotBlank(message = "不能为空")
    private String companyYearKey;

    private Integer carbonMarketIncluded;
    private Integer localTransitionPolicySupport;
    private Integer doubleCarbonCommitment;
    private Integer transitionPlanPrepared;
    private BigDecimal greenPatentRatio;
    private BigDecimal energyEnvironmentInvestmentIntensity;
    private BigDecimal nonTwoHighRevenueRatio;
    private BigDecimal digitalTechnologyLevel;
    private Integer executiveCompensationEsgLinked;
    private Integer thirdPartyVerification;
    private BigDecimal disclosureQuality;
    private BigDecimal powerSupplyCoalConsumption;
    private BigDecimal cleanEnergyInstalledCapacityRatio;
    private BigDecimal powerCarbonIntensity;
    private BigDecimal ccerDevelopmentVolume;
    private BigDecimal currentRatio;
    private BigDecimal revenueGrowthRate;
    private BigDecimal cashFlowRatio;
    private BigDecimal assetLiabilityRatio;
    private BigDecimal roa;
    private BigDecimal environmentalPenaltyCount;
    private BigDecimal industryProsperityIndex;
    private BigDecimal carbonPrice;

    /** 授信结果：1为已获授信，0为未获授信。 */
    @NotNull(message = "不能为空")
    private Integer loanGranted;

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
