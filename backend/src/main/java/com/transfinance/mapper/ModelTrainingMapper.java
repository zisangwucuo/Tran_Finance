package com.transfinance.mapper;

import com.transfinance.vo.IndustryModelVO;
import com.transfinance.vo.ModelTrainingJobVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模型训练任务与模型元数据 Mapper。
 */
public interface ModelTrainingMapper {

    /**
     * 查询当前金融机构最近一次训练任务。
     */
    @Select("""
            select id, job_type, status, progress, message, total_models, trained_models,
                   started_at, finished_at, created_at
             from model_training_job
             where institution_code = #{institutionCode}
               and job_type <> 'federated_train'
             order by created_at desc, id desc
             limit 1
            """)
    ModelTrainingJobVO selectLatestJob(@Param("institutionCode") String institutionCode);

    /**
     * 查询当前金融机构所有行业模型元数据。
     */
    @Select("""
            select industry_key, industry_name, target_name, model_type, sample_count, company_count,
                   year_start, year_end, model_version, status,
                   cast(metrics_json as char) as metrics_json,
                   cast(feature_weights_json as char) as feature_weights_json,
                   trained_at
              from industry_model_metadata
             where institution_code = #{institutionCode}
             order by trained_at desc, id desc
            """)
    List<IndustryModelVO> selectModels(@Param("institutionCode") String institutionCode);

    /**
     * 查询当前金融机构指定行业已就绪模型的元数据，用于生成报告中的模型权重说明。
     */
    @Select("""
            select industry_key, industry_name, target_name, model_type, sample_count, company_count,
                   year_start, year_end, model_version, status,
                   cast(metrics_json as char) as metrics_json,
                   cast(feature_weights_json as char) as feature_weights_json,
                   trained_at
              from industry_model_metadata
             where institution_code = #{institutionCode}
               and industry_key = #{industryKey}
               and target_name = 'verified_transition_performance'
               and status = 'READY'
             order by trained_at desc, id desc
             limit 1
            """)
    IndustryModelVO selectReadyIndustryModel(@Param("institutionCode") String institutionCode,
                                             @Param("industryKey") String industryKey);

    /**
     * 查询当前金融机构指定行业模型是否已就绪。
     */
    @Select("""
            select count(1)
              from industry_model_metadata
             where institution_code = #{institutionCode}
               and industry_key = #{industryKey}
               and target_name = 'verified_transition_performance'
               and status = 'READY'
            """)
    Integer countReadyIndustryModel(@Param("institutionCode") String institutionCode,
                                    @Param("industryKey") String industryKey);

    /**
     * 查询当前金融机构已训练模型文件路径，用于清空模型时逐个删除明确文件。
     */
    @Select("""
            select model_path
              from industry_model_metadata
             where institution_code = #{institutionCode}
               and model_path is not null
               and model_path <> ''
            """)
    List<String> selectModelPaths(@Param("institutionCode") String institutionCode);

    /**
     * 删除当前金融机构模型元数据。
     */
    @Delete("""
            delete from industry_model_metadata
             where institution_code = #{institutionCode}
            """)
    int deleteModels(@Param("institutionCode") String institutionCode);

    /**
     * 删除当前金融机构训练任务记录。清空模型后训练任务卡片应回到未开始状态。
     */
    @Delete("""
            delete from model_training_job
             where institution_code = #{institutionCode}
               and job_type <> 'federated_train'
            """)
    int deleteTrainingJobs(@Param("institutionCode") String institutionCode);
}
