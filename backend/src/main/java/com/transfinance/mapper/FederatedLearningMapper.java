package com.transfinance.mapper;

import com.transfinance.vo.FederatedModelVO;
import com.transfinance.vo.ModelTrainingJobVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 联邦学习任务与联邦模型元数据 Mapper。
 */
public interface FederatedLearningMapper {

    /**
     * 查询最近一次联邦学习训练任务。
     */
    @Select("""
            select id, job_type, status, progress, message, total_models, trained_models,
                   started_at, finished_at, created_at
              from model_training_job
             where job_type = 'federated_train'
             order by created_at desc, id desc
             limit 1
            """)
    ModelTrainingJobVO selectLatestFederatedJob();

    /**
     * 查询已经生成的行业联邦模型。
     */
    @Select("""
            select industry_key, industry_name, target_name, model_type, participant_count,
                   sample_count, company_count, year_start, year_end, model_version, status,
                   cast(metrics_json as char) as metrics_json,
                   cast(feature_weights_json as char) as feature_weights_json,
                   cast(participant_summary_json as char) as participant_summary_json,
                   trained_by, trained_at
              from federated_model_metadata
             order by trained_at desc, id desc
            """)
    List<FederatedModelVO> selectFederatedModels();

    /**
     * 查询指定行业已就绪联邦模型的元数据，用于生成报告中的模型权重说明。
     */
    @Select("""
            select industry_key, industry_name, target_name, model_type, participant_count,
                   sample_count, company_count, year_start, year_end, model_version, status,
                   cast(metrics_json as char) as metrics_json,
                   cast(feature_weights_json as char) as feature_weights_json,
                   cast(participant_summary_json as char) as participant_summary_json,
                   trained_by, trained_at
              from federated_model_metadata
             where industry_key = #{industryKey}
               and target_name = 'verified_transition_performance'
               and status = 'READY'
             order by trained_at desc, id desc
             limit 1
            """)
    FederatedModelVO selectReadyFederatedModel(@Param("industryKey") String industryKey);

    /**
     * 查询联邦模型文件路径，清空时逐个明确文件删除。
     */
    @Select("""
            select model_path
              from federated_model_metadata
             where model_path is not null
               and model_path <> ''
            """)
    List<String> selectFederatedModelPaths();

    /**
     * 删除联邦模型元数据。
     */
    @Delete("""
            delete from federated_model_metadata
            """)
    int deleteFederatedModels();

    /**
     * 删除联邦学习训练任务记录。
     */
    @Delete("""
            delete from model_training_job
             where job_type = 'federated_train'
            """)
    int deleteFederatedJobs();
}
