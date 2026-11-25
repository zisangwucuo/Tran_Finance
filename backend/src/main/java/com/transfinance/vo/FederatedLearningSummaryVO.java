package com.transfinance.vo;

import java.util.List;

/**
 * 联邦学习页面汇总数据。
 */
public class FederatedLearningSummaryVO {

    private ModelTrainingJobVO latestJob;
    private List<FederatedModelVO> models;

    public ModelTrainingJobVO getLatestJob() {
        return latestJob;
    }

    public void setLatestJob(ModelTrainingJobVO latestJob) {
        this.latestJob = latestJob;
    }

    public List<FederatedModelVO> getModels() {
        return models;
    }

    public void setModels(List<FederatedModelVO> models) {
        this.models = models;
    }
}
