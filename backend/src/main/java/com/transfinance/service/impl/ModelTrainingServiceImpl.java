package com.transfinance.service.impl;

import com.transfinance.exception.BusinessException;
import com.transfinance.dto.ModelParameters;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.mapper.ModelTrainingMapper;
import com.transfinance.service.CompanyMissingDataService;
import com.transfinance.service.ModelTrainingService;
import com.transfinance.vo.ModelTrainingSummaryVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 模型训练服务实现。
 */
@Service
public class ModelTrainingServiceImpl implements ModelTrainingService {

    /** 当前后端兼容的 Python 模型服务版本。 */
    private static final String REQUIRED_ML_SERVICE_VERSION = "0.5.0";

    private final ModelTrainingMapper modelTrainingMapper;
    private final FinancialInstitutionMapper institutionMapper;
    private final CompanyMissingDataService companyMissingDataService;
    private final RestClient restClient;
    private final String mlServiceUrl;

    public ModelTrainingServiceImpl(ModelTrainingMapper modelTrainingMapper,
                                    FinancialInstitutionMapper institutionMapper,
                                    CompanyMissingDataService companyMissingDataService,
                                    RestClient restClient,
                                    @Value("${ml.service-url:http://127.0.0.1:8090}") String mlServiceUrl) {
        this.modelTrainingMapper = modelTrainingMapper;
        this.institutionMapper = institutionMapper;
        this.companyMissingDataService = companyMissingDataService;
        this.restClient = restClient;
        this.mlServiceUrl = mlServiceUrl;
    }

    @Override
    public ModelTrainingSummaryVO summary(String institutionId) {
        requireInstitution(institutionId);
        ModelTrainingSummaryVO summary = new ModelTrainingSummaryVO();
        summary.setLatestJob(modelTrainingMapper.selectLatestJob(institutionId));
        summary.setModels(modelTrainingMapper.selectModels(institutionId));
        return summary;
    }

    @Override
    public Object trainAll(String institutionId, boolean force) {
        requireInstitution(institutionId);
        if (!force) {
            ensureNoMissingTrainingData(institutionId);
        }
        return callTrainApi(institutionId, false, force, ModelParameters.defaults());
    }

    @Override
    public Object updateModels(String institutionId, boolean force, ModelParameters parameters) {
        requireInstitution(institutionId);
        if (!force) {
            ensureNoMissingTrainingData(institutionId);
        }
        return callTrainApi(institutionId, true, force, parameters == null ? ModelParameters.defaults() : parameters);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object clearModels(String institutionId) {
        requireInstitution(institutionId);
        int deletedFiles = 0;
        for (String modelPath : modelTrainingMapper.selectModelPaths(institutionId)) {
            if (deleteOneModelFile(modelPath)) {
                deletedFiles++;
            }
        }
        int deletedModels = modelTrainingMapper.deleteModels(institutionId);
        int deletedJobs = modelTrainingMapper.deleteTrainingJobs(institutionId);
        return Map.of("deletedModels", deletedModels, "deletedFiles", deletedFiles, "deletedJobs", deletedJobs);
    }

    @Override
    public Object notifyCompanies(String institutionId) {
        requireInstitution(institutionId);
        int missingRows = companyMissingDataService.scanAndNotifyMissingData(institutionId);
        return Map.of("missingRows", missingRows);
    }

    private void ensureNoMissingTrainingData(String institutionId) {
        int missingRows = companyMissingDataService.countMissingTrainingData(institutionId);
        if (missingRows > 0) {
            throw new BusinessException("训练集发现 " + missingRows + " 条存在缺失字段的数据，请先点击“通知企业补全”，等待企业补全后再训练模型");
        }
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
            throw new BusinessException("模型文件删除失败，请检查文件权限");
        }
    }

    private Object callTrainApi(String institutionId, boolean updateMode, boolean force, ModelParameters parameters) {
        ensureMlServiceCompatible();
        try {
            return restClient.post()
                    .uri(mlServiceUrl + "/ml/train-all")
                    .body(Map.of(
                            "institutionId", institutionId,
                            "updateMode", updateMode,
                            "force", force,
                            "parameters", parameters
                    ))
                    .retrieve()
                    .body(Object.class);
        } catch (RestClientException exception) {
            throw new BusinessException("模型服务暂不可用，请先启动 Python 模型服务");
        }
    }

    /**
     * 训练前校验 Python 服务是否已经切换到当前行业模型版本。
     * 旧版本仍会训练绿色贷款模型或全行业模型，和当前数据库结构不兼容，必须阻止其创建任务。
     */
    private void ensureMlServiceCompatible() {
        try {
            Map<?, ?> health = restClient.get()
                    .uri(mlServiceUrl + "/health")
                    .retrieve()
                    .body(Map.class);
            Object version = health == null ? null : health.get("version");
            if (!REQUIRED_ML_SERVICE_VERSION.equals(String.valueOf(version))) {
                throw new BusinessException(
                        "Python 模型服务版本过旧或未返回版本信息，请停止后重启当前项目的 Python 模型服务"
                );
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
}
