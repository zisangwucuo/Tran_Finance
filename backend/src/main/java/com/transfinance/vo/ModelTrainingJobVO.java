package com.transfinance.vo;

import java.time.LocalDateTime;

/**
 * 模型训练任务展示对象。
 */
public class ModelTrainingJobVO {

    private Long id;
    private String jobType;
    private String status;
    private Integer progress;
    private String message;
    private Integer totalModels;
    private Integer trainedModels;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getTotalModels() {
        return totalModels;
    }

    public void setTotalModels(Integer totalModels) {
        this.totalModels = totalModels;
    }

    public Integer getTrainedModels() {
        return trainedModels;
    }

    public void setTrainedModels(Integer trainedModels) {
        this.trainedModels = trainedModels;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
