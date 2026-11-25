package com.transfinance.service;

import com.transfinance.vo.ModelTrainingSummaryVO;

/**
 * 模型训练服务。
 */
public interface ModelTrainingService {

    /**
     * 查询模型训练设置页汇总信息。
     *
     * @param institutionId 金融机构编码
     * @return 训练任务与模型列表
     */
    ModelTrainingSummaryVO summary(String institutionId);

    /**
     * 发起一键训练。
     *
     * @param institutionId 金融机构编码
     * @return Python 服务返回的任务数据
     */
    Object trainAll(String institutionId, boolean force);

    /**
     * 发起更新模型。
     *
     * @param institutionId 金融机构编码
     * @return Python 服务返回的任务数据
     */
    Object updateModels(String institutionId, boolean force, com.transfinance.dto.ModelParameters parameters);

    /**
     * 清空当前金融机构已训练模型。
     */
    Object clearModels(String institutionId);

    /**
     * 扫描缺失训练数据并通知企业补全。
     */
    Object notifyCompanies(String institutionId);
}
