from __future__ import annotations

import json
import os
from datetime import datetime
from pathlib import Path
from typing import Any, Literal

import joblib
import pandas as pd
import pymysql
from fastapi import BackgroundTasks, FastAPI, HTTPException
from pydantic import BaseModel, Field
from sklearn.dummy import DummyClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, log_loss, roc_auc_score
from sklearn.model_selection import train_test_split


SERVICE_VERSION = "0.5.0"
app = FastAPI(title="Trans Finance ML Service", version=SERVICE_VERSION)

MODEL_DIR = Path(os.getenv("MODEL_DIR", Path(__file__).parent / "models"))
TARGET_NAME = "verified_transition_performance"
TARGET_LABEL = "经核验转型绩效"


class ModelParameters(BaseModel):
    """与 Java DTO 同步的超参数边界，防止直接调用模型服务绕过校验。"""

    nEstimators: int = Field(300, ge=50, le=1000, strict=True)
    maxDepth: int = Field(8, ge=1, le=30, strict=True)
    minSamplesLeaf: int = Field(2, ge=1, le=50, strict=True)
    minSamplesSplit: int = Field(2, ge=2, le=100, strict=True)
    maxFeatures: Literal["sqrt", "log2", "all"] = "sqrt"
    classWeight: Literal["balanced", "balanced_subsample", "none"] = "balanced"
    randomState: int = Field(42, ge=0, le=2147483647, strict=True)

    def classifier_options(self) -> dict[str, Any]:
        """将界面枚举转为 sklearn 参数；全部特征和无权重均使用 None。"""
        return dict(n_estimators=self.nEstimators, max_depth=self.maxDepth,
                    min_samples_leaf=self.minSamplesLeaf, min_samples_split=self.minSamplesSplit,
                    max_features=None if self.maxFeatures == "all" else self.maxFeatures,
                    class_weight=None if self.classWeight == "none" else self.classWeight,
                    random_state=self.randomState, n_jobs=-1)


class TrainRequest(BaseModel):
    """模型训练请求。"""

    institutionId: str = Field(..., min_length=1)
    updateMode: bool = False
    force: bool = False
    parameters: ModelParameters = Field(default_factory=ModelParameters)


class PredictRequest(BaseModel):
    """行业模型预测请求。"""

    institutionId: str = Field(..., min_length=1)
    industryKey: str = Field(..., min_length=1)
    data: dict[str, Any]


class ComparisonRequest(BaseModel):
    """独立对比实验只接受机构和行业，不接收任意表名。"""
    institutionId: str = Field(..., min_length=1)
    industryKey: str = "all"
    mode: Literal["default", "tuned", "optimized"] = "default"


@app.post("/ml/comparisons")
def start_comparison(request: ComparisonRequest):
    import comparison
    if request.industryKey != "all" and request.industryKey not in INDUSTRIES:
        raise HTTPException(status_code=400, detail="行业不存在")
    try:
        return comparison.start(request.institutionId, request.industryKey, INDUSTRIES,
                                fetch_training_rows, features_for, normalize_value, request.mode)
    except ValueError as error:
        raise HTTPException(status_code=409, detail=str(error)) from error


@app.get("/ml/comparisons")
def comparison_history(institutionId: str):
    import comparison
    return comparison.history(institutionId)


class FederatedTrainRequest(BaseModel):
    """联邦随机森林集成训练请求。"""

    institutionId: str = Field(..., min_length=1)


class FederatedPredictRequest(BaseModel):
    """联邦行业模型预测请求。"""

    industryKey: str = Field(..., min_length=1)
    data: dict[str, Any]


class JobResponse(BaseModel):
    """训练任务创建结果。"""

    jobId: int
    status: str
    message: str


class PredictResponse(BaseModel):
    """转型贷款概率与评分。"""

    verifiedTransitionPerformanceProbability: float
    transitionScore: float


INDUSTRIES = {
    "nonferrous": ("有色金属行业", "nonferrous_metal_company_data"),
    "power": ("电力行业", "power_company_data"),
    "steel": ("钢铁行业", "steel_company_data"),
    "aviation": ("航空机场行业", "aviation_airport_company_data"),
    "chemical": ("基础化工行业", "basic_chemical_company_data"),
    "building": ("建材行业", "building_material_company_data"),
    "coal": ("煤炭行业", "coal_company_data"),
    "petrochemical": ("石油石化行业", "petrochemical_company_data"),
}

# 全行业通用指标。指标先按指标字典中的标杆/参考值归一化到 0-100，
# 再交给随机森林训练，避免不同量纲直接影响模型。
COMMON_FEATURES = [
    ("transitionTargetCommitment", "transition_target_commitment", "boolean"),
    ("transitionStrategyPath", "transition_strategy_path", "boolean"),
    ("governanceIncentive", "governance_incentive", "boolean"),
    ("transitionTechnologyApplication", "transition_technology_application", "boolean"),
    ("carbonAssetManagement", "carbon_asset_management", "boolean"),
    ("riskManagementMarketOpportunity", "risk_management_market_opportunity", "boolean"),
    ("informationDisclosureTransparency", "information_disclosure_transparency", "boolean"),
    ("justTransition", "just_transition", "boolean"),
    ("environmentalCompliance", "environmental_compliance", "boolean"),
    ("salesNetProfitMargin", "sales_net_profit_margin", "range", 20, 0, "higher"),
    ("revenueGrowthRate", "revenue_growth_rate", "range", 30, -10, "higher"),
    ("rAndDIntensity", "r_and_d_intensity", "range", 10, 0, "higher"),
]

# 各行业专项指标。每个金融机构在每个行业训练一个模型，
# 因此同名字段会按当前行业自己的参考区间归一化。
SPECIAL_FEATURES = {
    "steel": [
        ("blastFurnaceEnergyConsumption", "blast_furnace_energy_consumption", "range", 361, 460, "lower"),
        ("converterEnergyConsumption", "converter_energy_consumption", "range", -30, 5, "lower"),
        ("electricArcFurnaceEnergyConsumption", "electric_arc_furnace_energy_consumption", "range", 62, 95, "lower"),
        ("steelCarbonEmissionIntensity", "steel_carbon_emission_intensity", "range", 0.6, 2.3, "lower"),
        ("electricFurnaceShortProcessRatio", "electric_furnace_short_process_ratio", "range", 40, 3, "higher"),
        ("scrapSteelUse", "scrap_steel_use", "range", 450, 60, "higher"),
        ("byproductGasUtilizationRatio", "byproduct_gas_utilization_ratio", "range", 99.5, 90, "higher"),
    ],
    "power": [
        ("cleanEnergyInstalledCapacityRatio", "clean_energy_installed_capacity_ratio", "range", 90, 5, "higher"),
        ("cleanEnergyGenerationRatio", "clean_energy_generation_ratio", "range", 95, 5, "higher"),
        ("powerSupplyStandardCoalConsumption", "power_supply_standard_coal_consumption", "range", 270, 320, "lower"),
        ("powerGenerationCarbonIntensity", "power_generation_carbon_intensity", "range", 0.05, 1, "lower"),
        ("coalPowerFlexibilityRetrofitRatio", "coal_power_flexibility_retrofit_ratio", "range", 80, 5, "higher"),
        ("minimumTechnicalOutputPeakDepth", "minimum_technical_output_peak_depth", "range", 20, 50, "lower"),
        ("renewableEnergyInvestment", "renewable_energy_investment", "range", 200, 0.5, "higher"),
    ],
    "chemical": [
        ("syntheticAmmoniaEnergyConsumption", "synthetic_ammonia_energy_consumption", "range", 1350, 1650, "lower"),
        ("ethyleneNaphthaCrackingEnergyConsumption", "ethylene_naphtha_cracking_energy_consumption", "range", 590, 680, "lower"),
        ("methanolEnergyConsumption", "methanol_energy_consumption", "range", 1250, 1600, "lower"),
        ("productCarbonEmissionIntensity", "product_carbon_emission_intensity", "range", 1.5, 4, "lower"),
        ("greenPowerUseRatio", "green_power_use_ratio", "range", 50, 3, "higher"),
        ("greenHydrogenBiomassNonFossilFeedstockRatio", "green_hydrogen_biomass_non_fossil_feedstock_ratio", "range", 20, 0, "higher"),
        ("industrialWaterReuseRatio", "industrial_water_reuse_ratio", "range", 99, 92, "higher"),
    ],
    "petrochemical": [
        ("refiningEnergyConsumptionByEnergyFactor", "refining_energy_consumption_by_energy_factor", "range", 7, 9, "lower"),
        ("refiningCarbonEmissionIntensity", "refining_carbon_emission_intensity", "range", 0.15, 0.35, "lower"),
        ("methaneEmissionIntensity", "methane_emission_intensity", "range", 0.5, 3, "lower"),
        ("chemicalProductYieldRatio", "chemical_product_yield_ratio", "range", 55, 25, "higher"),
        ("cleanEnergyUseRatio", "clean_energy_use_ratio", "range", 20, 2, "higher"),
        ("sustainableAviationFuelBiofuelOutputSales", "sustainable_aviation_fuel_biofuel_output_sales", "range", 50, 0, "higher"),
    ],
    "building": [
        ("clinkerEnergyConsumption", "clinker_energy_consumption", "range", 1, 125, "lower"),
        ("flatGlassEnergyConsumption", "flat_glass_energy_consumption", "range", 8.5, 14, "lower"),
        ("productCarbonEmissionIntensity", "product_carbon_emission_intensity", "range", 0.6, 0.9, "lower"),
        ("alternativeFuelThermalSubstitutionRatio", "alternative_fuel_thermal_substitution_ratio", "range", 20, 1, "higher"),
        ("alternativeRawMaterialUseRatio", "alternative_raw_material_use_ratio", "range", 30, 2, "higher"),
    ],
    "nonferrous": [
        ("electrolyticAluminumRenewablePowerRatio", "electrolytic_aluminum_renewable_power_ratio", "range", 50, 5, "higher"),
        ("electrolyticAluminumDcPowerConsumption", "electrolytic_aluminum_dc_power_consumption", "range", 12500, 13500, "lower"),
        ("recycledMetalSupplyRatio", "recycled_metal_supply_ratio", "range", 40, 3, "higher"),
        ("recycledAluminumOutput", "recycled_aluminum_output", "range", 80, 0.5, "higher"),
        ("aluminumCarbonEmissionIntensity", "aluminum_carbon_emission_intensity", "range", 2, 13, "lower"),
        ("moltenAluminumDirectAlloyingRatio", "molten_aluminum_direct_alloying_ratio", "range", 95, 70, "higher"),
    ],
    "coal": [
        ("coalMineGasExtractionUtilizationRatio", "coal_mine_gas_extraction_utilization_ratio", "range", 65, 20, "higher"),
        ("rawCoalEnergyConsumption", "raw_coal_energy_consumption", "range", 3, 8, "lower"),
        ("mineWaterReuseRatio", "mine_water_reuse_ratio", "range", 95, 55, "higher"),
        ("gangueFlyAshReuseRatio", "gangue_fly_ash_reuse_ratio", "range", 90, 55, "higher"),
        ("nonCoalRevenueRatio", "non_coal_revenue_ratio", "range", 40, 2, "higher"),
        ("newEnergyInstalledCapacity", "new_energy_installed_capacity", "range", 3000, 0, "higher"),
    ],
    "aviation": [
        ("aviationFuelConsumptionPerTonKm", "aviation_fuel_consumption_per_ton_km", "range", 0.28, 0.34, "lower"),
        ("aviationCo2EmissionPerTonKm", "aviation_co2_emission_per_ton_km", "range", 0.85, 1, "lower"),
        ("co2EmissionPerRpk", "co2_emission_per_rpk", "range", 78, 95, "lower"),
        ("safUsage", "saf_usage", "range", 3000, 0, "higher"),
        ("passengerThroughputEnergyConsumption", "passenger_throughput_energy_consumption", "range", 0.8, 1, "lower"),
        ("passengerThroughputCo2Emission", "passenger_throughput_co2_emission", "range", 0.4, 0.55, "lower"),
        ("renewableEnergyConsumptionRatio", "renewable_energy_consumption_ratio", "range", 15, 2, "higher"),
        ("groundVehicleElectrificationRatio", "ground_vehicle_electrification_ratio", "range", 70, 10, "higher"),
    ],
}

ENVIRONMENTAL_RATING_FEATURE = (
    "environmentalPerformanceRating",
    "environmental_performance_rating",
    "rating",
)

# 同名专项指标可能出现在多个行业，但指标字典给出的参考范围可能不同。
# 归一化时按当前行业选用对应定义。
INDUSTRY_FEATURE_DEFS = {
    industry_key: {
        item[0]: item
        for item in [*COMMON_FEATURES, *SPECIAL_FEATURES[industry_key], ENVIRONMENTAL_RATING_FEATURE]
    }
    for industry_key in INDUSTRIES
}


@app.get("/health")
def health() -> dict[str, str]:
    """模型服务健康检查。

    后端在调用训练或预测之前，会先检查这个接口返回的 version，
    用来确保当前启动的是支持“转型贷款评分”和“联邦随机森林”的版本。
    """

    return {"status": "ok", "version": SERVICE_VERSION}


@app.post("/ml/train-all", response_model=JobResponse)
def train_all(request: TrainRequest, background_tasks: BackgroundTasks) -> JobResponse:
    """为当前金融机构创建八个行业模型训练任务。

    这里不直接执行训练，而是先写入一条训练任务记录，再交给后台任务异步跑。
    这样前端可以立即拿到 jobId，轮询进度而不会阻塞 HTTP 请求。
    """

    job_type = "update" if request.updateMode else "train_all"
    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                insert into model_training_job
                    (institution_code, job_type, status, progress, message, total_models, trained_models, started_at)
                values
                    (%s, %s, 'PENDING', 0, '训练任务已创建', %s, 0, now())
                """,
                (request.institutionId, job_type, len(INDUSTRIES)),
            )
            job_id = int(cursor.lastrowid)
    # 一键训练固定基线；更新任务持有独立参数快照，八个行业沿用同一配置。
    parameters = request.parameters if request.updateMode else ModelParameters()
    background_tasks.add_task(run_training_job, job_id, request.institutionId, parameters)
    return JobResponse(jobId=job_id, status="PENDING", message="行业模型训练任务已提交")


@app.post("/ml/predict", response_model=PredictResponse)
def predict(request: PredictRequest) -> PredictResponse:
    """使用当前金融机构的行业模型预测转型贷款概率。

    预测结果只输出概率与百分制评分，不直接改写数据库；
    真正的授信归档由 Java 后端在点击“授信”时完成。
    """

    if request.industryKey not in INDUSTRIES:
        raise HTTPException(status_code=400, detail="行业不存在")
    probability = predict_transition(request.institutionId, request.industryKey, request.data)
    return PredictResponse(
        verifiedTransitionPerformanceProbability=probability,
        transitionScore=round(probability * 100, 2),
    )


@app.post("/ml/federated/train", response_model=JobResponse)
def train_federated(request: FederatedTrainRequest, background_tasks: BackgroundTasks) -> JobResponse:
    """按行业聚合各金融机构本地随机森林，创建横向联邦学习训练任务。

    联邦训练只读各机构已经训练好的模型元数据和模型文件，不读取原始企业数据，
    这样可以保证“共享权重，不共享数据”的业务边界。
    """

    ensure_federated_tables()
    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                insert into model_training_job
                    (institution_code, job_type, status, progress, message, total_models, trained_models, started_at)
                values
                    (%s, 'federated_train', 'PENDING', 0, '联邦学习任务已创建', %s, 0, now())
                """,
                (request.institutionId, len(INDUSTRIES)),
            )
            job_id = int(cursor.lastrowid)
    background_tasks.add_task(run_federated_training_job, job_id, request.institutionId)
    return JobResponse(jobId=job_id, status="PENDING", message="联邦随机森林集成任务已提交")


@app.post("/ml/federated/predict", response_model=PredictResponse)
def predict_federated(request: FederatedPredictRequest) -> PredictResponse:
    """使用行业联邦随机森林集成模型预测转型贷款概率。

    这里的联邦预测是对多个本地模型的概率做加权平均，
    权重来源于各金融机构模型对应的样本量。
    """

    if request.industryKey not in INDUSTRIES:
        raise HTTPException(status_code=400, detail="行业不存在")
    probability = predict_federated_transition(request.industryKey, request.data)
    return PredictResponse(
        verifiedTransitionPerformanceProbability=probability,
        transitionScore=round(probability * 100, 2),
    )


def run_training_job(job_id: int, institution_code: str, parameters: ModelParameters | None = None) -> None:
    """执行八个行业模型任务，并只把异常类型写入任务状态。

    训练任务会按行业逐个执行，每训练完一个行业就更新一次进度，
    方便前端显示进度条和模型状态，而不是等全部结束后才返回结果。
    """

    trained = 0
    total = len(INDUSTRIES)
    try:
        # 先清理旧版本元数据，避免页面上同时看到旧绿色贷款模型和新转型贷款模型。
        cleanup_obsolete_metadata(institution_code)
        update_job(job_id, "RUNNING", 1, "正在训练行业模型", total, trained)
        for industry_key in INDUSTRIES:
            # 每个机构、每个行业单独训练一个本地随机森林模型。
            train_single_model(institution_code, industry_key, parameters)
            trained += 1
            progress = int(trained / total * 100)
            update_job(job_id, "RUNNING", progress, f"已完成 {trained}/{total} 个行业模型", total, trained)
        update_job(job_id, "SUCCESS", 100, "八个行业模型训练完成", total, trained, finished=True)
    except Exception as exc:
        update_job(
            job_id,
            "FAILED",
            100,
            f"模型训练失败：{exc.__class__.__name__}",
            total,
            trained,
            finished=True,
        )


def run_federated_training_job(job_id: int, initiator_code: str) -> None:
    """聚合同一行业下各金融机构已训练好的随机森林模型。

    聚合过程中会跳过缺少本地模型的行业，但不会因为单个行业缺失而立即中断全部任务，
    这样页面可以看到“部分成功、部分跳过”的真实状态。
    """

    trained = 0
    skipped: list[str] = []
    total = len(INDUSTRIES)
    try:
        update_job(job_id, "RUNNING", 1, "正在聚合联邦随机森林模型", total, trained)
        for industry_key, (industry_name, _) in INDUSTRIES.items():
            try:
                aggregate_federated_industry_model(industry_key, initiator_code)
                trained += 1
            except ValueError as exc:
                skipped.append(f"{industry_name}:{exc}")
            progress = int((trained + len(skipped)) / total * 100)
            update_job(
                job_id,
                "RUNNING",
                progress,
                f"已处理 {trained + len(skipped)}/{total} 个行业，成功 {trained} 个",
                total,
                trained,
            )
        if trained == 0:
            raise ValueError("没有可聚合的行业模型，请先让至少两家金融机构完成本地行业模型训练")
        message = f"联邦学习完成，成功聚合 {trained}/{total} 个行业模型"
        if skipped:
            message += f"，跳过 {len(skipped)} 个行业"
        update_job(job_id, "SUCCESS", 100, message, total, trained, finished=True)
    except Exception as exc:
        update_job(
            job_id,
            "FAILED",
            100,
            f"联邦学习失败：{exc.__class__.__name__}",
            total,
            trained,
            finished=True,
        )


def train_single_model(institution_code: str, industry_key: str, parameters: ModelParameters | None = None) -> None:
    """用单个行业历史年份数据训练当前金融机构的行业模型。

    训练逻辑里会把最后一年留给待评估数据使用，其余年份作为训练样本，
    这样才能模拟“历史数据训练模型、最新数据做评估”的真实业务流程。
    """

    industry_name, table_name = INDUSTRIES[industry_key]
    rows = fetch_training_rows(institution_code, table_name)
    if not rows:
        raise ValueError(f"{industry_name}没有可用历史训练数据")

    frame = pd.DataFrame(rows)
    if TARGET_NAME not in frame.columns:
        raise ValueError(f"{industry_name}历史表缺少转型贷款目标变量")

    target_values = pd.to_numeric(frame[TARGET_NAME], errors="coerce")
    valid_target = target_values.isin([0, 1])
    frame = frame.loc[valid_target].copy()
    target_values = target_values.loc[valid_target]
    if frame.empty:
        raise ValueError(f"{industry_name}历史数据没有有效的转型贷款目标变量")

    # 最后一年的数据留给待评估页或后续授信页使用，避免把待评估样本混进训练集。
    max_year = int(frame["data_year"].max())
    training_frame = frame[frame["data_year"] < max_year].copy()
    training_target = target_values.loc[training_frame.index]
    if training_frame.empty:
        training_frame = frame.copy()
        training_target = target_values

    feature_defs = features_for(industry_key)
    x = build_feature_frame(training_frame, feature_defs)
    y = training_target.astype(int)
    if x.empty or y.empty:
        raise ValueError(f"{industry_name}训练样本为空")

    # 训练随机森林之前先根据特征定义做归一化，保证不同量纲的指标可以共同进入模型。
    parameters = parameters or ModelParameters()
    model, metrics = fit_classifier(x, y, parameters)
    version = datetime.now().strftime("%Y%m%d%H%M%S")
    model_path = model_file_path(institution_code, industry_key, version)
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    # 随机森林的 feature_importances_ 会被保存下来，供评估报告和雷达图展示贡献度。
    feature_weights = build_feature_weights(model, feature_defs)
    joblib.dump(
        {
            "model": model,
            "feature_defs": feature_defs,
            "feature_columns": list(x.columns),
            "feature_weights": feature_weights,
            "training_parameters": parameters.model_dump(),
        },
        model_path,
    )

    metrics.update(
        {
            "targetLabel": TARGET_LABEL,
            "trainingParameters": parameters.model_dump(),
            "modelScope": industry_key,
            "maxExcludedYear": max_year,
            "missingValueStrategy": "blocked_by_backend_or_zero_when_forced",
            "featureCount": len(feature_defs),
            "positiveCount": int(y.sum()),
            "negativeCount": int((y == 0).sum()),
        }
    )
    save_model_metadata(
        institution_code=institution_code,
        industry_key=industry_key,
        industry_name=industry_name,
        model_path=str(model_path),
        feature_defs=feature_defs,
        feature_weights=feature_weights,
        metrics=metrics,
        sample_count=len(training_frame),
        company_count=training_frame["company_name"].nunique(),
        year_start=int(training_frame["data_year"].min()),
        year_end=int(training_frame["data_year"].max()),
        version=version,
    )


def fetch_training_rows(institution_code: str, table_name: str) -> list[dict[str, Any]]:
    """查询某金融机构在某行业的历史训练数据。"""

    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                "select institution_name from financial_institution where institution_code = %s",
                (institution_code,),
            )
            institution = cursor.fetchone()
            if not institution:
                raise ValueError("金融机构不存在")
            institution_name = institution["institution_name"]
            cursor.execute(
                f"""
                select *
                  from `{table_name}`
                 where institution_code = %s
                    or (coalesce(institution_code, '') = '' and institution_name = %s)
                 order by data_year, company_name, id
                """,
                (institution_code, institution_name),
            )
            return list(cursor.fetchall())


def fit_classifier(x: pd.DataFrame, y: pd.Series, parameters: ModelParameters | None = None):
    """训练随机森林；目标只有一个类别时使用哑分类器保证任务可完成。

    真实业务里有些金融机构某个行业的样本可能全是正例或全是负例，
    这时普通随机森林无法训练，只能退回到 DummyClassifier 保证流程不阻断。
    """

    parameters = parameters or ModelParameters()
    if y.nunique() < 2:
        model = DummyClassifier(strategy="constant", constant=int(y.iloc[0]))
        model.fit(x, y)
        return model, {
            "accuracy": 1.0,
            "auc": None,
            "logLoss": None,
            "validationMode": "single_class",
        }

    class_counts = y.value_counts()
    can_holdout = len(y) >= 20 and class_counts.min() >= 2
    # 样本量足够时再切分训练集和验证集，避免过小数据集导致验证集类分布失衡。
    if can_holdout:
        x_train, x_test, y_train, y_test = train_test_split(
            x,
            y,
            test_size=0.25,
            random_state=42,
            stratify=y,
        )
        validation_mode = "holdout"
    else:
        x_train, x_test, y_train, y_test = x, x, y, y
        validation_mode = "training"

    # 这里保留随机森林作为主模型，兼顾非线性拟合能力和特征重要性可解释性。
    # 保持验证集切分种子不变，便于在相同数据上比较不同森林参数。
    model = RandomForestClassifier(**parameters.classifier_options())
    model.fit(x_train, y_train)
    # 用测试集概率计算模型质量指标，页面里的“训练结果”就是从这里来的。
    probabilities = probability_for_class_one(model, x_test)
    predictions = (probabilities >= 0.5).astype(int)
    metrics = {
        "accuracy": round(float(accuracy_score(y_test, predictions)), 4),
        "auc": safe_auc(y_test, probabilities),
        "logLoss": safe_log_loss(y_test, probabilities),
        "validationMode": validation_mode,
    }
    return model, metrics


def predict_transition(institution_code: str, industry_key: str, data: dict[str, Any]) -> float:
    """加载行业模型并预测一条待评估数据。

    预测时只拼出模型需要的字段，再按训练时保存的 feature_columns 顺序补齐，
    防止前端字段顺序变化影响模型输入。
    """

    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                select model_path
                  from industry_model_metadata
                 where institution_code = %s
                   and industry_key = %s
                   and target_name = 'verified_transition_performance'
                   and status = 'READY'
                 limit 1
                """,
                (institution_code, industry_key),
            )
            metadata = cursor.fetchone()
    if not metadata:
        raise HTTPException(status_code=400, detail="当前行业模型尚未训练")

    model_path = Path(metadata["model_path"])
    if not model_path.is_file():
        raise HTTPException(status_code=400, detail="模型文件不可用，请重新训练")
    try:
        # joblib 文件里同时保存了模型本体、特征定义和训练时的列顺序。
        bundle = joblib.load(model_path)
        prediction_data = {**data, "_industry_key": industry_key}
        feature_frame = build_feature_frame(pd.DataFrame([prediction_data]), bundle["feature_defs"])
        feature_frame = feature_frame.reindex(columns=bundle["feature_columns"], fill_value=0)
        probability = probability_for_class_one(bundle["model"], feature_frame)[0]
        return round(float(probability), 4)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=f"模型预测失败：{exc.__class__.__name__}",
        ) from exc


def features_for(industry_key: str) -> list[tuple[Any, ...]]:
    """返回某行业模型需要的通用指标和专项指标。"""

    return [*COMMON_FEATURES, *SPECIAL_FEATURES[industry_key], ENVIRONMENTAL_RATING_FEATURE]


def build_feature_frame(frame: pd.DataFrame, feature_defs: list[tuple[Any, ...]]) -> pd.DataFrame:
    """按特征定义将原始值归一化，并用 0 表示强制训练时的缺失值。

    这里返回的是模型真正吃进去的数值矩阵，而不是数据库里的原始值。
    """

    data: dict[str, pd.Series] = {}
    for definition in feature_defs:
        prop = definition[0]
        data[prop] = frame.apply(
            lambda row: normalize_feature_row(row, definition),
            axis=1,
        )
    return pd.DataFrame(data, index=frame.index).fillna(0).clip(lower=0, upper=100)


def normalize_feature_row(row: pd.Series, definition: tuple[Any, ...]) -> float:
    """按行所属行业选择同名指标的正确归一化参数。

    同一个指标在不同细分行业里参考区间可能不同，所以这里会先看 `_industry_key`，
    再决定应该用哪一组 best/lag 参数。
    """

    prop = definition[0]
    industry_key = row.get("_industry_key")
    industry_definition = INDUSTRY_FEATURE_DEFS.get(industry_key, {}).get(prop, definition)
    _, column, kind, *rest = industry_definition
    if prop in row.index:
        value = row[prop]
    elif column in row.index:
        value = row[column]
    else:
        value = None
    return normalize_value(kind, value, rest)


def normalize_value(kind: str, value: Any, rest: list[Any]) -> float:
    """将布尔、评级和有方向数值统一映射到 0-100。

    模型内部统一使用 0-100 的分值，便于后续计算特征贡献和雷达图。
    """

    if value is None or pd.isna(value) or value == "":
        return 0.0
    if kind == "boolean":
        if isinstance(value, str):
            return 100.0 if value.strip() in {"是", "1", "true", "True"} else 0.0
        return 100.0 if int(value) == 1 else 0.0
    if kind == "rating":
        text = str(value)
        if "A" in text:
            return 100.0
        if "B" in text:
            return 70.0
        if "C" in text:
            return 35.0
        return 0.0
    try:
        numeric = float(value)
    except (TypeError, ValueError):
        return 0.0
    if kind == "score":
        return clamp(numeric if numeric > 1 else numeric * 100)

    best, lag, direction = rest
    if direction == "lower":
        denominator = float(lag) - float(best)
        return clamp((float(lag) - numeric) / denominator * 100) if denominator else 0.0
    denominator = float(best) - float(lag)
    return clamp((numeric - float(lag)) / denominator * 100) if denominator else 0.0


def build_feature_weights(model: Any, feature_defs: list[tuple[Any, ...]]) -> list[dict[str, Any]]:
    """保存随机森林特征重要性，供后续计算指标贡献度使用。

    这些权重会写入数据库，前端报告页和模型设置页都依赖它来展示“前三名贡献指标”。
    """

    importances = getattr(model, "feature_importances_", None)
    if importances is None:
        importances = [0.0] * len(feature_defs)
    total = float(sum(importances))
    if total > 0:
        normalized = [float(value) / total for value in importances]
    else:
        normalized = [0.0] * len(feature_defs)

    weights = []
    for definition, weight in zip(feature_defs, normalized):
        prop, column, *_ = definition
        weights.append(
            {
                "prop": prop,
                "column": column,
                "weight": round(weight, 8),
            }
        )
    return weights


def probability_for_class_one(model: Any, x: pd.DataFrame):
    probabilities = model.predict_proba(x)
    classes = list(model.classes_)
    if 1 in classes:
        return probabilities[:, classes.index(1)]
    return pd.Series([0.0] * len(x)).to_numpy()


def safe_auc(y_true: pd.Series, probabilities) -> float | None:
    try:
        if y_true.nunique() < 2:
            return None
        return round(float(roc_auc_score(y_true, probabilities)), 4)
    except ValueError:
        return None


def safe_log_loss(y_true: pd.Series, probabilities) -> float | None:
    try:
        return round(float(log_loss(y_true, probabilities, labels=[0, 1])), 4)
    except ValueError:
        return None


def clamp(value: float) -> float:
    if not pd.notna(value):
        return 0.0
    return max(0.0, min(100.0, float(value)))


def model_file_path(institution_code: str, industry_key: str, version: str) -> Path:
    safe_scope = f"{institution_code}_{industry_key}_{TARGET_NAME}_{version}".replace("/", "_").replace("\\", "_")
    return MODEL_DIR / f"{safe_scope}.joblib"


def save_model_metadata(
    institution_code: str,
    industry_key: str,
    industry_name: str,
    model_path: str,
    feature_defs: list[tuple[Any, ...]],
    feature_weights: list[dict[str, Any]],
    metrics: dict[str, Any],
    sample_count: int,
    company_count: int,
    year_start: int,
    year_end: int,
    version: str,
) -> None:
    """写入行业模型元数据与特征权重。"""

    feature_columns = [item[0] for item in feature_defs]
    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                insert into industry_model_metadata
                    (institution_code, industry_key, industry_name, target_name, model_type, model_path,
                     feature_columns_json, feature_weights_json, metrics_json, sample_count, company_count,
                     year_start, year_end, model_version, status, trained_at)
                values
                    (%s, %s, %s, %s, 'RandomForestClassifier', %s,
                     cast(%s as json), cast(%s as json), cast(%s as json), %s, %s, %s, %s,
                     %s, 'READY', now())
                on duplicate key update
                    industry_name = values(industry_name),
                    model_type = values(model_type),
                    model_path = values(model_path),
                    feature_columns_json = values(feature_columns_json),
                    feature_weights_json = values(feature_weights_json),
                    metrics_json = values(metrics_json),
                    sample_count = values(sample_count),
                    company_count = values(company_count),
                    year_start = values(year_start),
                    year_end = values(year_end),
                    model_version = values(model_version),
                    status = values(status),
                    trained_at = values(trained_at)
                """,
                (
                    institution_code,
                    industry_key,
                    industry_name,
                    TARGET_NAME,
                    model_path,
                    json.dumps(feature_columns, ensure_ascii=False),
                    json.dumps(feature_weights, ensure_ascii=False),
                    json.dumps(metrics, ensure_ascii=False),
                    sample_count,
                    company_count,
                    year_start,
                    year_end,
                    version,
                ),
            )


def cleanup_obsolete_metadata(institution_code: str) -> None:
    """清理旧版绿色贷款模型和全行业模型元数据，避免页面混入旧模型记录。"""

    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                delete from industry_model_metadata
                 where institution_code = %s
                   and (industry_key = 'all' or target_name <> 'verified_transition_performance')
                """,
                (institution_code,),
            )


def aggregate_federated_industry_model(industry_key: str, initiator_code: str) -> None:
    """将同一行业的多个本地随机森林模型组合成一个联邦集成模型。

    这里不是共享原始样本，而是把各机构的本地模型按样本量权重合成一个集成模型，
    这样可以保留机构本地数据边界，同时让同一行业的预测更稳定。
    """

    industry_name, _ = INDUSTRIES[industry_key]
    local_models = load_local_model_metadata(industry_key)
    if len(local_models) < 2:
        raise ValueError("可用本地模型少于2个")

    feature_defs = features_for(industry_key)
    feature_columns = [item[0] for item in feature_defs]
    readable_models = [
        item
        for item in local_models
        if Path(item["model_path"]).is_file()
    ]
    if len(readable_models) < 2:
        raise ValueError("可读取的本地模型少于2个")

    # 样本越多的机构在联邦聚合中权重越高，避免小样本模型对整体结果影响过大。
    total_samples = sum(max(int(item.get("sample_count") or 0), 1) for item in readable_models)
    members = []
    participant_summary = []
    weighted_feature_scores = {prop: 0.0 for prop in feature_columns}
    weighted_metrics: dict[str, float] = {}
    metric_weights: dict[str, float] = {}
    company_count = 0
    year_start_values = []
    year_end_values = []

    for item in readable_models:
        model_path = Path(item["model_path"])
        # 每个成员模型都保留自己的列顺序，聚合时会按这个顺序补齐特征向量。
        bundle = joblib.load(model_path)
        sample_count = max(int(item.get("sample_count") or 0), 1)
        weight = sample_count / total_samples if total_samples else 0.0
        members.append(
            {
                "institutionCode": item["institution_code"],
                "institutionName": item.get("institution_name") or item["institution_code"],
                "weight": weight,
                "sampleCount": sample_count,
                "model": bundle["model"],
                "featureColumns": bundle.get("feature_columns", feature_columns),
            }
        )
        participant_summary.append(
            {
                "institutionCode": item["institution_code"],
                "institutionName": item.get("institution_name") or item["institution_code"],
                "sampleCount": int(item.get("sample_count") or 0),
                "companyCount": int(item.get("company_count") or 0),
                "weight": round(weight, 8),
            }
        )
        company_count += int(item.get("company_count") or 0)
        if item.get("year_start") is not None:
            year_start_values.append(int(item["year_start"]))
        if item.get("year_end") is not None:
            year_end_values.append(int(item["year_end"]))
        merge_feature_weights(weighted_feature_scores, bundle.get("feature_weights", []), weight)
        merge_metrics(weighted_metrics, metric_weights, item.get("metrics_json"), weight)

    feature_weights = [
        {
            "prop": definition[0],
            "column": definition[1],
            "weight": round(weighted_feature_scores.get(definition[0], 0.0), 8),
        }
        for definition in feature_defs
    ]
    metrics = finalize_weighted_metrics(weighted_metrics, metric_weights)
    metrics.update(
        {
            "aggregationMethod": "weighted_random_forest_ensemble",
            "participantCount": len(members),
            "targetLabel": TARGET_LABEL,
            "modelScope": industry_key,
        }
    )

    version = datetime.now().strftime("%Y%m%d%H%M%S")
    model_path = federated_model_file_path(industry_key, version)
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(
        {
            "modelType": "FederatedRandomForestEnsemble",
            "industryKey": industry_key,
            "feature_defs": feature_defs,
            "feature_columns": feature_columns,
            "feature_weights": feature_weights,
            "members": members,
        },
        model_path,
    )
    save_federated_model_metadata(
        industry_key=industry_key,
        industry_name=industry_name,
        model_path=str(model_path),
        feature_defs=feature_defs,
        feature_weights=feature_weights,
        metrics=metrics,
        participant_summary=participant_summary,
        sample_count=sum(item["sampleCount"] for item in participant_summary),
        company_count=company_count,
        year_start=min(year_start_values) if year_start_values else None,
        year_end=max(year_end_values) if year_end_values else None,
        version=version,
        trained_by=initiator_code,
    )


def load_local_model_metadata(industry_key: str) -> list[dict[str, Any]]:
    """读取指定行业已经训练好的各金融机构本地模型元数据。"""

    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                select m.institution_code, fi.institution_name, m.model_path, m.metrics_json,
                       m.sample_count, m.company_count, m.year_start, m.year_end
                  from industry_model_metadata m
                  left join financial_institution fi on fi.institution_code = m.institution_code
                 where m.industry_key = %s
                   and m.target_name = 'verified_transition_performance'
                   and m.status = 'READY'
                   and m.model_path is not null
                   and m.model_path <> ''
                 order by m.sample_count desc, m.trained_at desc
                """,
                (industry_key,),
            )
            return list(cursor.fetchall())


def merge_feature_weights(target: dict[str, float], weights: list[dict[str, Any]], model_weight: float) -> None:
    """按样本量权重聚合各本地随机森林的特征重要性。"""

    if not isinstance(weights, list):
        return
    for item in weights:
        prop = item.get("prop")
        if prop in target:
            target[prop] += float(item.get("weight") or 0.0) * model_weight


def merge_metrics(
    weighted_metrics: dict[str, float],
    metric_weights: dict[str, float],
    metrics_json: Any,
    model_weight: float,
) -> None:
    """聚合可数值化的本地验证指标，作为联邦模型的参考指标。"""

    try:
        metrics = json.loads(metrics_json) if isinstance(metrics_json, str) else metrics_json
    except (TypeError, json.JSONDecodeError):
        return
    if not isinstance(metrics, dict):
        return
    for key in ("accuracy", "auc", "logLoss"):
        value = metrics.get(key)
        if value is None:
            continue
        weighted_metrics[key] = weighted_metrics.get(key, 0.0) + float(value) * model_weight
        metric_weights[key] = metric_weights.get(key, 0.0) + model_weight


def finalize_weighted_metrics(weighted_metrics: dict[str, float], metric_weights: dict[str, float]) -> dict[str, Any]:
    """将加权指标转成页面可展示的 JSON。"""

    result: dict[str, Any] = {}
    for key, value in weighted_metrics.items():
        weight = metric_weights.get(key, 0.0)
        result[key] = round(value / weight, 4) if weight else None
    return result


def predict_federated_transition(industry_key: str, data: dict[str, Any]) -> float:
    """加载行业联邦模型，并按各参与模型样本权重平均预测概率。

    这里返回的是“联邦平均概率”，再由 Java 后端乘以 100 转成评分。
    """

    ensure_federated_tables()
    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                select model_path
                  from federated_model_metadata
                 where industry_key = %s
                   and target_name = 'verified_transition_performance'
                   and status = 'READY'
                 limit 1
                """,
                (industry_key,),
            )
            metadata = cursor.fetchone()
    if not metadata:
        raise HTTPException(status_code=400, detail="当前行业联邦模型尚未训练")

    model_path = Path(metadata["model_path"])
    if not model_path.is_file():
        raise HTTPException(status_code=400, detail="联邦模型文件不可用，请重新训练")
    try:
        bundle = joblib.load(model_path)
        prediction_data = {**data, "_industry_key": industry_key}
        feature_frame = build_feature_frame(pd.DataFrame([prediction_data]), bundle["feature_defs"])
        probability = 0.0
        weight_sum = 0.0
        for member in bundle.get("members", []):
            member_frame = feature_frame.reindex(columns=member["featureColumns"], fill_value=0)
            member_probability = probability_for_class_one(member["model"], member_frame)[0]
            member_weight = float(member.get("weight") or 0.0)
            probability += float(member_probability) * member_weight
            weight_sum += member_weight
        if weight_sum <= 0:
            raise ValueError("联邦模型参与方权重无效")
        return round(probability / weight_sum, 4)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=400,
            detail=f"联邦模型预测失败：{exc.__class__.__name__}",
        ) from exc


def federated_model_file_path(industry_key: str, version: str) -> Path:
    safe_scope = f"federated_{industry_key}_{TARGET_NAME}_{version}".replace("/", "_").replace("\\", "_")
    return MODEL_DIR / f"{safe_scope}.joblib"


def save_federated_model_metadata(
    industry_key: str,
    industry_name: str,
    model_path: str,
    feature_defs: list[tuple[Any, ...]],
    feature_weights: list[dict[str, Any]],
    metrics: dict[str, Any],
    participant_summary: list[dict[str, Any]],
    sample_count: int,
    company_count: int,
    year_start: int | None,
    year_end: int | None,
    version: str,
    trained_by: str,
) -> None:
    """保存联邦模型元数据，页面只展示聚合信息，不展示企业原始数据。"""

    feature_columns = [item[0] for item in feature_defs]
    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                insert into federated_model_metadata
                    (industry_key, industry_name, target_name, model_type, model_path,
                     feature_columns_json, feature_weights_json, metrics_json, participant_summary_json,
                     participant_count, sample_count, company_count, year_start, year_end,
                     model_version, status, trained_by, trained_at)
                values
                    (%s, %s, %s, 'FederatedRandomForestEnsemble', %s,
                     cast(%s as json), cast(%s as json), cast(%s as json), cast(%s as json),
                     %s, %s, %s, %s, %s, %s, 'READY', %s, now())
                on duplicate key update
                    industry_name = values(industry_name),
                    model_type = values(model_type),
                    model_path = values(model_path),
                    feature_columns_json = values(feature_columns_json),
                    feature_weights_json = values(feature_weights_json),
                    metrics_json = values(metrics_json),
                    participant_summary_json = values(participant_summary_json),
                    participant_count = values(participant_count),
                    sample_count = values(sample_count),
                    company_count = values(company_count),
                    year_start = values(year_start),
                    year_end = values(year_end),
                    model_version = values(model_version),
                    status = values(status),
                    trained_by = values(trained_by),
                    trained_at = values(trained_at)
                """,
                (
                    industry_key,
                    industry_name,
                    TARGET_NAME,
                    model_path,
                    json.dumps(feature_columns, ensure_ascii=False),
                    json.dumps(feature_weights, ensure_ascii=False),
                    json.dumps(metrics, ensure_ascii=False),
                    json.dumps(participant_summary, ensure_ascii=False),
                    len(participant_summary),
                    sample_count,
                    company_count,
                    year_start,
                    year_end,
                    version,
                    trained_by,
                ),
            )


def ensure_federated_tables() -> None:
    """保证联邦模型元数据表存在，避免首次打开页面时需要手工建表。"""

    with connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                create table if not exists `federated_model_metadata` (
                  `id` bigint primary key auto_increment comment '联邦模型元数据ID',
                  `industry_key` varchar(64) not null comment '行业模型编码',
                  `industry_name` varchar(100) not null comment '行业名称',
                  `target_name` varchar(64) not null comment '目标变量',
                  `model_type` varchar(100) not null comment '模型类型',
                  `model_path` varchar(500) not null comment '模型文件路径',
                  `feature_columns_json` json null comment '特征字段列表',
                  `feature_weights_json` json null comment '聚合特征重要性',
                  `metrics_json` json null comment '聚合评估指标',
                  `participant_summary_json` json null comment '参与机构摘要',
                  `participant_count` int not null default 0 comment '参与机构数量',
                  `sample_count` int not null default 0 comment '聚合样本数',
                  `company_count` int not null default 0 comment '聚合企业数',
                  `year_start` int null comment '训练集起始年份',
                  `year_end` int null comment '训练集结束年份',
                  `model_version` varchar(64) not null comment '模型版本',
                  `status` varchar(30) not null comment '模型状态',
                  `trained_by` varchar(64) null comment '发起训练的机构编码',
                  `trained_at` datetime null comment '训练完成时间',
                  `updated_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
                  unique key `uk_federated_model_scope` (`industry_key`, `target_name`),
                  key `idx_federated_model_status` (`status`, `trained_at`)
                ) engine=InnoDB default charset=utf8mb4 comment='联邦行业模型元数据表'
                """
            )


def db_config() -> dict[str, Any]:
    """读取模型服务数据库环境变量，不在代码中保存数据库密码。"""

    password = os.getenv("DB_PASSWORD")
    if password is None:
        raise RuntimeError("缺少 DB_PASSWORD 环境变量")
    return {
        "host": os.getenv("DB_HOST", "127.0.0.1"),
        "port": int(os.getenv("DB_PORT", "3306")),
        "user": os.getenv("DB_USERNAME", "root"),
        "password": password,
        "database": os.getenv("DB_NAME", "trans_finance"),
        "charset": "utf8mb4",
        "cursorclass": pymysql.cursors.DictCursor,
        "autocommit": True,
    }


def connection():
    return pymysql.connect(**db_config())


def update_job(
    job_id: int,
    status: str,
    progress: int,
    message: str,
    total_models: int,
    trained_models: int,
    finished: bool = False,
) -> None:
    """更新训练进度，消息中不包含请求体、密码或数据库凭据。"""

    finished_sql = ", finished_at = now()" if finished else ""
    with connection() as conn:
        with conn.cursor() as cursor:
            sql = f"""
                update model_training_job
                   set status = %s,
                       progress = %s,
                       message = %s,
                       total_models = %s,
                       trained_models = %s
                       {finished_sql}
                 where id = %s
            """
            cursor.execute(sql, (status, progress, message, total_models, trained_models, job_id))
