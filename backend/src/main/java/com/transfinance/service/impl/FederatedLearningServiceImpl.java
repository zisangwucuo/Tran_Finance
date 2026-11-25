package com.transfinance.service.impl;

import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.FederatedLearningMapper;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.service.FederatedLearningService;
import com.transfinance.vo.FederatedLearningSummaryVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 联邦学习服务实现。
 */
@Service
public class FederatedLearningServiceImpl implements FederatedLearningService {

    /** 当前后端兼容的 Python 模型服务版本。 */
    private static final String REQUIRED_ML_SERVICE_VERSION = "0.5.0";

    private final FederatedLearningMapper federatedLearningMapper;
    private final FinancialInstitutionMapper institutionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String mlServiceUrl;

    public FederatedLearningServiceImpl(FederatedLearningMapper federatedLearningMapper,
                                        FinancialInstitutionMapper institutionMapper,
                                        JdbcTemplate jdbcTemplate,
                                        RestClient restClient,
                                        @Value("${ml.service-url:http://127.0.0.1:8090}") String mlServiceUrl) {
        this.federatedLearningMapper = federatedLearningMapper;
        this.institutionMapper = institutionMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = restClient;
        this.mlServiceUrl = mlServiceUrl;
    }

    @Override
    public FederatedLearningSummaryVO summary() {
        ensureFederatedTable();
        FederatedLearningSummaryVO summary = new FederatedLearningSummaryVO();
        summary.setLatestJob(federatedLearningMapper.selectLatestFederatedJob());
        summary.setModels(federatedLearningMapper.selectFederatedModels());
        return summary;
    }

    @Override
    public Object train(String institutionId) {
        ensureFederatedTable();
        requireInstitution(institutionId);
        ensureMlServiceCompatible();
        try {
            return restClient.post()
                    .uri(mlServiceUrl + "/ml/federated/train")
                    .body(Map.of("institutionId", institutionId))
                    .retrieve()
                    .body(Object.class);
        } catch (RestClientException exception) {
            throw new BusinessException("模型服务暂不可用，请先启动 Python 模型服务");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object clear() {
        ensureFederatedTable();
        int deletedFiles = 0;
        for (String modelPath : federatedLearningMapper.selectFederatedModelPaths()) {
            if (deleteOneModelFile(modelPath)) {
                deletedFiles++;
            }
        }
        int deletedModels = federatedLearningMapper.deleteFederatedModels();
        int deletedJobs = federatedLearningMapper.deleteFederatedJobs();
        return Map.of("deletedModels", deletedModels, "deletedFiles", deletedFiles, "deletedJobs", deletedJobs);
    }

    private boolean deleteOneModelFile(String modelPath) {
        try {
            Path path = Path.of(modelPath).normalize();
            if (path.getFileName() == null || !path.getFileName().toString().endsWith(".joblib")) {
                return false;
            }
            if (!Files.isRegularFile(path)) {
                return false;
            }
            Files.deleteIfExists(path);
            return true;
        } catch (Exception exception) {
            throw new BusinessException("联邦模型文件删除失败，请检查文件权限");
        }
    }

    /**
     * 校验 Python 服务版本，避免旧模型服务没有联邦学习接口时产生误导。
     */
    private void ensureMlServiceCompatible() {
        try {
            Map<?, ?> health = restClient.get()
                    .uri(mlServiceUrl + "/health")
                    .retrieve()
                    .body(Map.class);
            Object version = health == null ? null : health.get("version");
            if (!REQUIRED_ML_SERVICE_VERSION.equals(String.valueOf(version))) {
                throw new BusinessException("Python 模型服务版本过旧，请重启当前项目的 Python 模型服务");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException("模型服务暂不可用，请先启动 Python 模型服务");
        }
    }

    private void requireInstitution(String institutionId) {
        if (institutionMapper.selectById(institutionId) == null) {
            throw new BusinessException("金融机构不存在，请重新登录");
        }
    }

    /**
     * 首次使用联邦学习页面时保证元数据表存在。
     */
    private void ensureFederatedTable() {
        jdbcTemplate.execute("""
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
                """);
    }
}
