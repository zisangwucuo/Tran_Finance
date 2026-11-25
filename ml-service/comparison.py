"""独立、可追溯的时间外模型对比，不写业务表或业务模型文件。"""
import hashlib
import json
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path
from time import perf_counter

import numpy as np
import pandas as pd
import sklearn
from sklearn.pipeline import Pipeline
from sklearn.base import clone
from sklearn.model_selection import ParameterSampler
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import (accuracy_score, roc_auc_score, average_precision_score,
                             precision_score, recall_score, f1_score, log_loss, brier_score_loss)

ROOT = Path(__file__).parent / "experiments"
LOCK = threading.Lock()
ACTIVE = set()
POOL = ThreadPoolExecutor(max_workers=1)


def bank_path(bank):
    # 目录只使用哈希，不允许用户输入参与路径拼接。
    return ROOT / hashlib.sha256(bank.encode()).hexdigest()


def save(bank, result):
    directory = bank_path(bank)
    directory.mkdir(parents=True, exist_ok=True)
    target = directory / (result["id"] + ".json")
    temporary = target.with_suffix(".tmp")
    temporary.write_text(json.dumps(result, ensure_ascii=False, allow_nan=False), encoding="utf-8")
    temporary.replace(target)


def history(bank):
    results = []
    for path in sorted(bank_path(bank).glob("*.json"), key=lambda p: p.stat().st_mtime, reverse=True)[:30]:
        result = json.loads(path.read_text(encoding="utf-8"))
        # 单进程服务重启后不把遗留任务永远显示为运行中。
        if result["status"] in ("PENDING", "RUNNING") and bank not in ACTIVE:
            result = {**result, "status": "FAILED", "message": "服务重启导致实验中断，请重新运行"}
        results.append(result)
    return results


def start(bank, industry, catalog, fetch, features, normalize, mode="default"):
    if mode not in ("default", "tuned", "optimized"):
        raise ValueError("不支持的对比模式")
    with LOCK:
        if bank in ACTIVE:
            raise ValueError("当前机构已有对比实验正在运行")
        ACTIVE.add(bank)
    result = dict(id=str(uuid.uuid4()), createdAt=datetime.now(timezone.utc).isoformat(),
                  status="PENDING", progress=0, results=[], message="等待实验执行",
                  protocol="temporal-holdout-v2" if mode == "optimized" else "temporal-holdout-v1", sklearnVersion=sklearn.__version__,
                  industryKey=industry, seed=42, threshold=0.5, mode=mode,
                  split="各行业最新历史年份测试，更早年份训练",
                  tuning="原始数值与缺失标记，每模型32组配置，训练年份内滚动验证选择AUC最高配置" if mode == "optimized" else
                  "每模型8组配置，训练年份内滚动验证选择AUC最高配置" if mode == "tuned" else "固定基线参数，未调参")
    try:
        save(bank, result)
        POOL.submit(run, bank, industry, result, catalog, fetch, features, normalize)
    except Exception:
        with LOCK:
            ACTIVE.discard(bank)
        raise
    return {"id": result["id"]}


def estimators():
    """固定基线参数，不利用测试集选择超参数；梯度提升不支持class_weight，明确记录。"""
    return [
        ("逻辑回归", LogisticRegression(C=1.0, max_iter=2000, class_weight="balanced", random_state=42)),
        ("单棵决策树", DecisionTreeClassifier(max_depth=8, min_samples_leaf=2, class_weight="balanced", random_state=42)),
        ("梯度提升树", GradientBoostingClassifier(n_estimators=100, learning_rate=0.1, max_depth=3, min_samples_leaf=2, random_state=42)),
        ("随机森林", RandomForestClassifier(n_estimators=300, max_depth=8, min_samples_leaf=2, class_weight="balanced", random_state=42, n_jobs=-1)),
    ]


def tune_pipeline(pipeline, name, x, y, years, progress=None, budget=8):
    """仅接收外层训练集；每折重新拟合填补和标准化，测试年份不进入搜索。"""
    folds = []
    for year in sorted(years.unique())[1:]:
        earlier, later = years < year, years == year
        if earlier.sum() >= 10 and y.loc[earlier].nunique() == 2 and y.loc[later].nunique() == 2:
            folds.append((earlier, later, int(year)))
    if not folds:
        raise ValueError("自动调参需要训练年份内至少一折有效双类别时间验证，请增加历史年份或使用默认模式")
    spaces = {
        "逻辑回归": {"C": [.01, .1, 1., 10., 100.], "class_weight": [None, "balanced"]},
        "单棵决策树": {"max_depth": [3, 5, 8, None], "min_samples_leaf": [1, 2, 4, 8], "class_weight": [None, "balanced"]},
        "梯度提升树": {"n_estimators": [100, 200], "learning_rate": [.03, .1], "max_depth": [2, 3], "min_samples_leaf": [2, 4, 8]},
        "随机森林": {"n_estimators": [200, 300], "max_depth": [4, 8, None], "min_samples_leaf": [1, 2, 4, 8],
                   "max_features": ["sqrt", .7, 1.0], "class_weight": [None, "balanced"]},
    }
    if budget == 32:
        # 预先固定扩展搜索空间，覆盖平滑叶节点和抽样强度；不根据测试集成绩调整空间。
        spaces = {
            "逻辑回归": {"C": np.logspace(-4, 4, 17).tolist(), "class_weight": [None, "balanced"]},
            "单棵决策树": {"max_depth": [2, 3, 5, 8, 12, None], "min_samples_leaf": [1, 2, 4, 8, 12],
                         "min_samples_split": [2, 8, 16], "class_weight": [None, "balanced"], "ccp_alpha": [0., .001, .01]},
            "梯度提升树": {"n_estimators": [100, 200, 400], "learning_rate": [.02, .05, .1], "max_depth": [1, 2, 3],
                         "min_samples_leaf": [2, 4, 8], "subsample": [.7, 1.]},
            "随机森林": {"n_estimators": [300, 600], "max_depth": [3, 5, 8, 12, None],
                       "min_samples_leaf": [1, 2, 4, 8, 12], "min_samples_split": [2, 8, 16],
                       "max_features": ["sqrt", .5, .8, 1.], "class_weight": [None, "balanced", "balanced_subsample"],
                       "max_samples": [.6, .8, None], "criterion": ["gini", "entropy"]},
        }
    # 每种模型含基线共8种不同配置，同分优先先出现的配置；不根据测试分数反复抽样。
    base = pipeline.named_steps["model"].get_params()
    candidates = [{key: base[key] for key in spaces[name]}]
    count = min(budget * 2, int(np.prod([len(values) for values in spaces[name].values()])))
    for candidate in ParameterSampler(spaces[name], n_iter=count, random_state=42):
        if candidate not in candidates:
            candidates.append(candidate)
        if len(candidates) == budget:
            break
    trials = []
    best_score, best_params = -1., {}
    for number, params in enumerate(candidates):
        scores = []
        for earlier, later, year in folds:
            estimator = clone(pipeline).set_params(**{"model__" + k: v for k, v in params.items()})
            estimator.fit(x.loc[earlier], y.loc[earlier])
            scores.append(float(roc_auc_score(y.loc[later], estimator.predict_proba(x.loc[later])[:, 1])))
        mean = float(np.mean(scores))
        trials.append(dict(parameters=params, validationAuc=mean, foldAuc=scores))
        if mean > best_score:
            best_score, best_params = mean, params
        if progress:
            progress(name, number + 1, len(candidates))
    pipeline.set_params(**{"model__" + k: v for k, v in best_params.items()})
    return dict(candidateCount=len(candidates), validationYears=[fold[2] for fold in folds],
                validationAuc=best_score, trials=trials, searchSpace=spaces[name])


def classification_result(truth, probabilities, threshold):
    """固定矩阵方向：真实类别为行，预测类别为列；未定义指标用null而非伪造0。"""
    truth = np.asarray(truth, dtype=int)
    predicted = np.asarray(probabilities) >= threshold
    tp = int(((truth == 1) & predicted).sum())
    fp = int(((truth == 0) & predicted).sum())
    fn = int(((truth == 1) & ~predicted).sum())
    tn = int(((truth == 0) & ~predicted).sum())
    return dict(threshold=float(threshold), predictedPositive=tp + fp,
                confusionMatrix=[[tn, fp], [fn, tp]],
                accuracy=float((tp + tn) / len(truth)),
                precision=tp / (tp + fp) if tp + fp else None,
                recall=tp / (tp + fn) if tp + fn else None,
                f1=2 * tp / (2 * tp + fp + fn) if 2 * tp + fp + fn else None,
                precisionReason="没有预测正例，精确率分母为0" if tp + fp == 0 else "",
                recallReason="测试集没有实际正例，召回率分母为0" if tp + fn == 0 else "")


def select_threshold(pipeline, x, y, years):
    """所有模型使用相同时间折，汇总折外预测选择F1；不接收最终测试数据。"""
    labels, probabilities, validation_years = [], [], []
    for year in sorted(years.unique())[1:]:
        earlier, later = years < year, years == year
        if earlier.sum() < 10 or y.loc[earlier].nunique() != 2 or y.loc[later].nunique() != 2:
            continue
        fitted = clone(pipeline).fit(x.loc[earlier], y.loc[earlier])
        labels.extend(y.loc[later].tolist())
        probabilities.extend(fitted.predict_proba(x.loc[later])[:, 1].tolist())
        validation_years.append(int(year))
    if not validation_years:
        return dict(available=False, reason="训练年份内无有效双类别时间验证折，未选择阈值")
    # 扫描每个不同概率形成的决策边界；同分优先离0.5最近，再选较高阈值。
    candidates = sorted(set(probabilities + [0.5]))
    scores = [(classification_result(labels, probabilities, value)["f1"], value) for value in candidates]
    score, threshold = max(scores, key=lambda item: (item[0], -abs(item[1] - .5), item[1]))
    return dict(available=True, threshold=threshold, validationF1=score, validationYears=validation_years,
                validationSamples=len(labels), rule="内部时间验证折外预测F1最大；同分优先接近0.5，再取较高阈值")


def feature_frame(frame, definitions, normalize, raw_values=False):
    """数值不截断；评级/布尔使用固定编码，非法值保留缺失，填补留给训练折内Pipeline。"""
    data = {}
    for prop, column, kind, *rest in definitions:
        raw = frame[column] if column in frame else pd.Series(np.nan, index=frame.index)
        values = []
        for value in raw:
            if pd.isna(value) or str(value).strip() == "":
                values.append(np.nan)
                continue
            if kind == "boolean" and not raw_values:
                values.append(normalize(kind, value, rest))
            elif kind == "boolean":
                key = str(value).strip().lower()
                encoded = {"1": 100., "1.0": 100., "true": 100., "是": 100.,
                           "0": 0., "0.0": 0., "false": 0., "否": 0.}.get(key, np.nan)
                values.append(encoded)
            elif kind == "rating":
                values.append(normalize(kind, value, rest))
            else:
                numeric = pd.to_numeric(value, errors="coerce")
                values.append(np.nan if not np.isfinite(numeric) else float(numeric) if raw_values else normalize(kind, value, rest))
        data[prop] = values
    return pd.DataFrame(data, index=frame.index)


def evaluate(rows, definitions, normalize, mode="default", progress=None):
    if mode not in ("default", "tuned", "optimized"):
        raise ValueError("不支持的对比模式")
    frame = pd.DataFrame(rows)
    if frame.empty or "verified_transition_performance" not in frame:
        raise ValueError("没有带目标变量的历史样本")
    labels = pd.to_numeric(frame["verified_transition_performance"], errors="coerce")
    years = pd.to_numeric(frame["data_year"], errors="coerce")
    valid = labels.isin([0, 1]) & years.notna() & (years % 1 == 0)
    excluded = int((~valid).sum())
    frame, labels, years = frame.loc[valid], labels.loc[valid].astype(int), years.loc[valid].astype(int)
    if years.nunique() < 2:
        raise ValueError("至少需要两个有效历史年份")
    if frame.duplicated(["company_name", "data_year"]).any():
        raise ValueError("存在重复企业年度记录，请先核对数据，避免重复样本影响比较")
    test_year = int(years.max())
    train, test = years < test_year, years == test_year
    if labels.loc[train].nunique() < 2:
        raise ValueError("训练集只有一个类别，无法公平比较二分类模型")
    if train.sum() < 10 or test.sum() < 2:
        raise ValueError("有效训练样本不足10条或测试样本不足2条")
    # 采用业务固定方向映射，但将缺失值保留为NaN，之后只在训练集拟合中位数。
    x = feature_frame(frame, definitions, normalize, raw_values=mode == "optimized")
    fingerprint = hashlib.sha256(pd.util.hash_pandas_object(
        pd.concat([frame[["company_name", "data_year"]], x, labels.rename("target")], axis=1), index=False
    ).values.tobytes()).hexdigest()
    context = dict(trainSamples=int(train.sum()), testSamples=int(test.sum()),
                   trainYears=sorted(years.loc[train].unique().tolist()), testYear=test_year,
                   trainCompanies=int(frame.loc[train, "company_name"].nunique()),
                   testCompanies=int(frame.loc[test, "company_name"].nunique()),
                   trainPositive=int(labels.loc[train].sum()), testPositive=int(labels.loc[test].sum()),
                   missingTrainValues=int(x.loc[train].isna().sum().sum()),
                   featureMode="raw_with_missing_indicators" if mode == "optimized" else "business_normalized",
                   excludedSamples=excluded, featureCount=len(definitions), dataFingerprint=fingerprint,
                   warning="测试集只有一个类别，AUC和AP不可计算" if labels.loc[test].nunique() < 2 else "")
    results = []
    for name, estimator in estimators():
        # 所有模型采用相同缺失值处理；仅逻辑回归增加训练集标准化。
        steps = [("imputer", SimpleImputer(strategy="median", keep_empty_features=True, add_indicator=mode == "optimized"))]
        if name == "逻辑回归":
            steps.append(("scale", StandardScaler()))
        pipeline = Pipeline(steps + [("model", estimator)])
        tuning = None
        search_started = perf_counter()
        if mode in ("tuned", "optimized"):
            tuning = tune_pipeline(pipeline, name, x.loc[train], labels.loc[train], years.loc[train], progress,
                                   budget=32 if mode == "optimized" else 8)
        search_seconds = perf_counter() - search_started if mode in ("tuned", "optimized") else 0.
        threshold_started = perf_counter()
        threshold_selection = select_threshold(pipeline, x.loc[train], labels.loc[train], years.loc[train])
        threshold_seconds = perf_counter() - threshold_started
        started = perf_counter()
        pipeline.fit(x.loc[train], labels.loc[train])
        fit_seconds = perf_counter() - started
        started = perf_counter()
        probabilities = pipeline.predict_proba(x.loc[test])[:, 1]
        prediction_ms = (perf_counter() - started) * 1000
        truth = labels.loc[test]
        fixed = classification_result(truth, probabilities, .5)
        selected_threshold_result = classification_result(truth, probabilities, threshold_selection["threshold"]) if threshold_selection["available"] else None
        predicted = (probabilities >= 0.5).astype(int)
        both = truth.nunique() == 2
        scores = dict(accuracy=accuracy_score(truth, predicted),
                      auc=roc_auc_score(truth, probabilities) if both else None,
                      ap=average_precision_score(truth, probabilities) if both else None,
                      precision=fixed["precision"],
                      recall=fixed["recall"],
                      f1=fixed["f1"],
                      logLoss=log_loss(truth, probabilities, labels=[0, 1]),
                      brier=brier_score_loss(truth, probabilities),
                      trainSeconds=fit_seconds, predictMs=prediction_ms)
        results.append(dict(model=name, **context, **{k: round(float(v), 6) if v is not None else None for k, v in scores.items()},
                            parameters=pipeline.named_steps["model"].get_params(), mode=mode,
                            fixedThreshold=fixed, selectedThreshold=selected_threshold_result,
                            thresholdSelection=threshold_selection, thresholdSeconds=round(threshold_seconds, 6),
                            probabilityRange=dict(min=float(np.min(probabilities)), max=float(np.max(probabilities)), mean=float(np.mean(probabilities))),
                            tuning=tuning, searchSeconds=round(search_seconds, 6)))
    return results


def run(bank, industry, result, catalog, fetch, features, normalize):
    selected = list(catalog) if industry == "all" else [industry]
    failures = []
    try:
        result.update(status="RUNNING", message="正在按年份划分并训练四种模型")
        save(bank, result)
        for index, key in enumerate(selected):
            try:
                def report(model, done, total):
                    result.update(message=f"{catalog[key][0]} · {model} · 已验证配置 {done}/{total}")
                    save(bank, result)
                rows = evaluate(fetch(bank, catalog[key][1]), features(key), normalize,
                                result.get("mode", "default"), report)
                result["results"].extend(dict(industryKey=key, industryName=catalog[key][0], **row) for row in rows)
            except ValueError as error:
                failures.append(dict(industryKey=key, reason=str(error)))
            except Exception as error:
                failures.append(dict(industryKey=key, reason="实验失败：" + type(error).__name__))
            result.update(progress=round((index + 1) / len(selected) * 100), failures=failures)
            save(bank, result)
        result.update(status="PARTIAL" if failures and result["results"] else "FAILED" if failures else "SUCCESS",
                      message="实验完成" if not failures else "部分行业未完成，请查看原因")
        save(bank, result)
    finally:
        with LOCK:
            ACTIVE.discard(bank)
