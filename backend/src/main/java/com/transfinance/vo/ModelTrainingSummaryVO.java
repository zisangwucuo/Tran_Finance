package com.transfinance.vo;

import java.util.List;

/**
 * 模型训练设置页汇总数据。
 */
public class ModelTrainingSummaryVO {

    private ModelTrainingJobVO latestJob;
    private List<IndustryModelVO> models;

    public ModelTrainingJobVO getLatestJob() {
        return latestJob;
    }

    public void setLatestJob(ModelTrainingJobVO latestJob) {
        this.latestJob = latestJob;
    }

    public List<IndustryModelVO> getModels() {
        return models;
    }

    public void setModels(List<IndustryModelVO> models) {
        this.models = models;
    }
}
