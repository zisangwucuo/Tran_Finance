package com.transfinance.service;

import com.transfinance.vo.FederatedLearningSummaryVO;

/**
 * 联邦学习服务。
 */
public interface FederatedLearningService {

    /**
     * 查询联邦学习训练任务和模型汇总。
     */
    FederatedLearningSummaryVO summary();

    /**
     * 发起横向联邦随机森林集成训练。
     */
    Object train(String institutionId);

    /**
     * 清空已经生成的联邦模型。
     */
    Object clear();
}
