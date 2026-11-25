package com.transfinance.service.impl;

import com.transfinance.common.PageResult;
import com.transfinance.dto.ElectricCompanyDataQueryRequest;
import com.transfinance.dto.IndustryCompanyDataSaveRequest;
import com.transfinance.entity.FinancialInstitution;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.mapper.IndustryCompanyDataMapper;
import com.transfinance.mapper.ModelTrainingMapper;
import com.transfinance.service.ElectricCompanyDataService;
import com.transfinance.vo.ColumnMetaVO;
import com.transfinance.vo.IndustryCompanyDataPageResponse;
import com.transfinance.vo.IndustryOptionVO;
import com.transfinance.vo.IndustryOptionsResponse;
import com.transfinance.vo.TransitionScoreTrendVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业评估数据服务实现。
 */
@Service
public class ElectricCompanyDataServiceImpl implements ElectricCompanyDataService {

    private static final List<ColumnDefinition> COMMON_COLUMNS = List.of(
            text("industryName", "industry_name", "行业", 140, "left"),
            text("companyCode", "company_code", "企业编码", 160, "left"),
            text("companyName", "company_name", "企业名称", 180, "left"),
            text("stockCode", "stock_code", "股票代码", 120, null),
            text("companyType", "company_type", "企业类型", 120, null),
            number("dataYear", "data_year", "年份", 100, null),
            bool("transitionTargetCommitment", "transition_target_commitment", "转型目标与承诺", 150),
            bool("transitionStrategyPath", "transition_strategy_path", "转型战略与路径规划", 170),
            bool("governanceIncentive", "governance_incentive", "治理架构与激励", 150),
            bool("transitionTechnologyApplication", "transition_technology_application", "转型技术应用现状", 170),
            bool("carbonAssetManagement", "carbon_asset_management", "碳资产管理能力", 150),
            bool("riskManagementMarketOpportunity", "risk_management_market_opportunity", "风险管理与市场机遇", 180),
            bool("informationDisclosureTransparency", "information_disclosure_transparency", "信息披露与透明度", 170),
            bool("justTransition", "just_transition", "公正转型", 120),
            bool("environmentalCompliance", "environmental_compliance", "环境合规", 120),
            number("salesNetProfitMargin", "sales_net_profit_margin", "销售净利率(%)", 150, null),
            number("revenueGrowthRate", "revenue_growth_rate", "营业收入增长率(%)", 170, null),
            number("rAndDIntensity", "r_and_d_intensity", "研发投入强度(%)", 160, null)
    );

    private static final List<ColumnDefinition> RESULT_COLUMNS = List.of(
            number("transitionScore", "transition_score", "转型综合评分", 140, null),
            bool("verifiedTransitionPerformance", "verified_transition_performance", "经核验转型绩效", 140)
    );

    private static final Map<String, IndustryDefinition> INDUSTRIES = createIndustryDefinitions();

    private final IndustryCompanyDataMapper dataMapper;
    private final FinancialInstitutionMapper institutionMapper;
    private final ModelTrainingMapper modelTrainingMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String mlServiceUrl;

    public ElectricCompanyDataServiceImpl(IndustryCompanyDataMapper dataMapper,
                                          FinancialInstitutionMapper institutionMapper,
                                          ModelTrainingMapper modelTrainingMapper,
                                          JdbcTemplate jdbcTemplate,
                                          RestClient restClient,
                                          @Value("${ml.service-url:http://127.0.0.1:8090}") String mlServiceUrl) {
        this.dataMapper = dataMapper;
        this.institutionMapper = institutionMapper;
        this.modelTrainingMapper = modelTrainingMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = restClient;
        this.mlServiceUrl = mlServiceUrl;
    }

    @Override
    public IndustryCompanyDataPageResponse page(ElectricCompanyDataQueryRequest request) {
        FinancialInstitution institution = requireInstitution(request.getInstitutionId());
        IndustryDefinition industry = requireIndustry(request.getIndustryKey());
        long offset = (request.getPage() - 1) * request.getSize();
        List<Map<String, Object>> rows = dataMapper.selectPageRows(
                industry.tableName(),
                industry.selectColumns(),
                institution.getInstitutionCode(),
                institution.getInstitutionName(),
                request.getCompanyName(),
                request.getCompanyCode(),
                request.getStockCode(),
                request.getDataYear(),
                request.getVerifiedTransitionPerformance(),
                offset,
                request.getSize()
        );
        Long total = dataMapper.countRows(
                industry.tableName(),
                institution.getInstitutionCode(),
                institution.getInstitutionName(),
                request.getCompanyName(),
                request.getCompanyCode(),
                request.getStockCode(),
                request.getDataYear(),
                request.getVerifiedTransitionPerformance()
        );

        IndustryCompanyDataPageResponse response = new IndustryCompanyDataPageResponse();
        response.setIndustryKey(industry.key());
        response.setIndustryName(industry.name());
        response.setColumns(industry.columns().stream().map(ColumnDefinition::toVO).collect(Collectors.toList()));
        response.setPage(new PageResult<>(rows, total == null ? 0 : total, request.getPage(), request.getSize()));
        return response;
    }

    @Override
    public IndustryCompanyDataPageResponse pendingPage(ElectricCompanyDataQueryRequest request) {
        FinancialInstitution institution = requireInstitution(request.getInstitutionId());
        IndustryDefinition industry = requireIndustry(request.getIndustryKey());
        long offset = (request.getPage() - 1) * request.getSize();
        List<Map<String, Object>> rows = dataMapper.selectPageRows(
                industry.pendingTableName(),
                industry.pendingSelectColumns(),
                institution.getInstitutionCode(),
                institution.getInstitutionName(),
                request.getCompanyName(),
                request.getCompanyCode(),
                request.getStockCode(),
                request.getDataYear(),
                null,
                offset,
                request.getSize()
        );
        Long total = dataMapper.countRows(
                industry.pendingTableName(),
                institution.getInstitutionCode(),
                institution.getInstitutionName(),
                request.getCompanyName(),
                request.getCompanyCode(),
                request.getStockCode(),
                request.getDataYear(),
                null
        );

        IndustryCompanyDataPageResponse response = new IndustryCompanyDataPageResponse();
        response.setIndustryKey(industry.key());
        response.setIndustryName(industry.name());
        response.setColumns(industry.pendingColumns().stream().map(ColumnDefinition::toVO).collect(Collectors.toList()));
        response.setPage(new PageResult<>(rows, total == null ? 0 : total, request.getPage(), request.getSize()));
        return response;
    }

    @Override
    public IndustryOptionsResponse options(String institutionId, String industryKey) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition selectedIndustry = requireIndustry(StringUtils.hasText(industryKey) ? industryKey : "power");

        List<IndustryOptionVO> industryOptions = INDUSTRIES.values().stream()
                .filter(industry -> {
                    Long count = dataMapper.countByInstitution(
                            industry.tableName(),
                            institution.getInstitutionCode(),
                            institution.getInstitutionName()
                    );
                    return count != null && count > 0;
                })
                .map(industry -> new IndustryOptionVO(industry.key(), industry.name()))
                .collect(Collectors.toList());

        IndustryOptionsResponse response = new IndustryOptionsResponse();
        response.setIndustries(industryOptions);
        response.setYears(dataMapper.selectYears(
                selectedIndustry.tableName(),
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        ));
        return response;
    }

    @Override
    public IndustryOptionsResponse pendingOptions(String institutionId, String industryKey) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition selectedIndustry = requireIndustry(StringUtils.hasText(industryKey) ? industryKey : "power");

        List<IndustryOptionVO> industryOptions = INDUSTRIES.values().stream()
                .filter(industry -> {
                    Long count = dataMapper.countByInstitution(
                            industry.pendingTableName(),
                            institution.getInstitutionCode(),
                            institution.getInstitutionName()
                    );
                    return count != null && count > 0;
                })
                .map(industry -> new IndustryOptionVO(industry.key(), industry.name()))
                .collect(Collectors.toList());

        IndustryOptionsResponse response = new IndustryOptionsResponse();
        response.setIndustries(industryOptions);
        response.setYears(dataMapper.selectYears(
                selectedIndustry.pendingTableName(),
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        ));
        return response;
    }

    @Override
    public void create(IndustryCompanyDataSaveRequest request) {
        FinancialInstitution institution = requireInstitution(request.getInstitutionId());
        IndustryDefinition industry = requireIndustry(request.getIndustryKey());
        Map<String, Object> params = buildWriteParams(industry, institution, request.getData(), true, true);
        List<ColumnDefinition> insertColumns = industry.insertColumns();
        String insertColumnSql = insertColumns.stream()
                .map(column -> "`" + column.column() + "`")
                .collect(Collectors.joining(", "));
        String valuePlaceholderSql = insertColumns.stream()
                .map(column -> "#{params." + column.prop() + "}")
                .collect(Collectors.joining(", "));
        dataMapper.insertRow(industry.tableName(), insertColumnSql, valuePlaceholderSql, params);
    }

    @Override
    public void createPending(IndustryCompanyDataSaveRequest request) {
        FinancialInstitution institution = requireInstitution(request.getInstitutionId());
        IndustryDefinition industry = requireIndustry(request.getIndustryKey());
        Map<String, Object> params = buildWriteParams(industry, institution, request.getData(), true, false);
        List<ColumnDefinition> insertColumns = industry.pendingInsertColumns();
        String insertColumnSql = insertColumns.stream()
                .map(column -> "`" + column.column() + "`")
                .collect(Collectors.joining(", "));
        String valuePlaceholderSql = insertColumns.stream()
                .map(column -> "#{params." + column.prop() + "}")
                .collect(Collectors.joining(", "));
        dataMapper.insertRow(industry.pendingTableName(), insertColumnSql, valuePlaceholderSql, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, IndustryCompanyDataSaveRequest request) {
        FinancialInstitution institution = requireInstitution(request.getInstitutionId());
        IndustryDefinition industry = requireIndustry(request.getIndustryKey());
        String sourceIndustryKey = StringUtils.hasText(request.getSourceIndustryKey())
                ? request.getSourceIndustryKey()
                : request.getIndustryKey();
        IndustryDefinition sourceIndustry = requireIndustry(sourceIndustryKey);
        Map<String, Object> params = buildWriteParams(industry, institution, request.getData(), false, true);
        if (sourceIndustry.key().equals(industry.key())) {
            List<ColumnDefinition> updateColumns = new ArrayList<>();
            updateColumns.add(text("industryName", "industry_name", "行业", 140, null));
            updateColumns.addAll(industry.writableColumns());
            String setSql = updateColumns.stream()
                    .map(column -> "`" + column.column() + "` = #{params." + column.prop() + "}")
                    .collect(Collectors.joining(", "));
            int affectedRows = dataMapper.updateRow(
                    industry.tableName(),
                    setSql,
                    id,
                    institution.getInstitutionCode(),
                    institution.getInstitutionName(),
                    params
            );
            if (affectedRows == 0) {
                throw new BusinessException("企业评估数据不存在或无权修改");
            }
            return;
        }
        int deletedRows = dataMapper.deleteRow(
                sourceIndustry.tableName(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (deletedRows == 0) {
            throw new BusinessException("企业评估数据不存在或无权修改");
        }
        List<ColumnDefinition> insertColumns = industry.insertColumns();
        String insertColumnSql = insertColumns.stream()
                .map(column -> "`" + column.column() + "`")
                .collect(Collectors.joining(", "));
        String valuePlaceholderSql = insertColumns.stream()
                .map(column -> "#{params." + column.prop() + "}")
                .collect(Collectors.joining(", "));
        dataMapper.insertRow(industry.tableName(), insertColumnSql, valuePlaceholderSql, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePending(Long id, IndustryCompanyDataSaveRequest request) {
        FinancialInstitution institution = requireInstitution(request.getInstitutionId());
        IndustryDefinition industry = requireIndustry(request.getIndustryKey());
        String sourceIndustryKey = StringUtils.hasText(request.getSourceIndustryKey())
                ? request.getSourceIndustryKey()
                : request.getIndustryKey();
        IndustryDefinition sourceIndustry = requireIndustry(sourceIndustryKey);
        Map<String, Object> params = buildWriteParams(industry, institution, request.getData(), false, false);
        if (sourceIndustry.key().equals(industry.key())) {
            List<ColumnDefinition> updateColumns = new ArrayList<>();
            updateColumns.add(text("industryName", "industry_name", "行业", 140, null));
            updateColumns.addAll(industry.pendingWritableColumns());
            String setSql = updateColumns.stream()
                    .map(column -> "`" + column.column() + "` = #{params." + column.prop() + "}")
                    .collect(Collectors.joining(", "));
            int affectedRows = dataMapper.updateRow(
                    industry.pendingTableName(),
                    setSql,
                    id,
                    institution.getInstitutionCode(),
                    institution.getInstitutionName(),
                    params
            );
            if (affectedRows == 0) {
                throw new BusinessException("待评估企业数据不存在或无权修改");
            }
            return;
        }
        int deletedRows = dataMapper.deleteRow(
                sourceIndustry.pendingTableName(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (deletedRows == 0) {
            throw new BusinessException("待评估企业数据不存在或无权修改");
        }
        List<ColumnDefinition> insertColumns = industry.pendingInsertColumns();
        String insertColumnSql = insertColumns.stream()
                .map(column -> "`" + column.column() + "`")
                .collect(Collectors.joining(", "));
        String valuePlaceholderSql = insertColumns.stream()
                .map(column -> "#{params." + column.prop() + "}")
                .collect(Collectors.joining(", "));
        dataMapper.insertRow(industry.pendingTableName(), insertColumnSql, valuePlaceholderSql, params);
    }

    @Override
    public void deleteById(String industryKey, String institutionId, Long id) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition industry = requireIndustry(industryKey);
        int affectedRows = dataMapper.deleteRow(
                industry.tableName(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (affectedRows == 0) {
            throw new BusinessException("企业评估数据不存在或无权删除");
        }
    }

    @Override
    public void deletePendingById(String industryKey, String institutionId, Long id) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition industry = requireIndustry(industryKey);
        int affectedRows = dataMapper.deleteRow(
                industry.pendingTableName(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (affectedRows == 0) {
            throw new BusinessException("待评估企业数据不存在或无权删除");
        }
    }

    @Override
    public void scorePending(Long id, String institutionId, String industryKey, String modelMode) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition industry = requireIndustry(industryKey);
        String normalizedModelMode = StringUtils.hasText(modelMode) ? modelMode : "local";
        boolean federatedMode = "federated".equals(normalizedModelMode);
        if (!federatedMode && !"local".equals(normalizedModelMode)) {
            throw new BusinessException("评分模型类型不正确");
        }
        if (!federatedMode) {
            Integer readyModels = modelTrainingMapper.countReadyIndustryModel(institution.getInstitutionCode(), industry.key());
            if (readyModels == null || readyModels < 1) {
                throw new BusinessException("当前金融机构的" + industry.name() + "模型尚未训练完成，暂不能开始评估");
            }
        }
        Map<String, Object> row = dataMapper.selectRowById(
                industry.pendingTableName(),
                industry.pendingSelectColumns(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (row == null) {
            throw new BusinessException("待评估企业数据不存在或无权评估");
        }
        Map<String, Object> prediction;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("industryKey", industry.key());
            payload.put("data", row);
            if (!federatedMode) {
                payload.put("institutionId", institution.getInstitutionCode());
            }
            prediction = restClient.post()
                    .uri(mlServiceUrl + (federatedMode ? "/ml/federated/predict" : "/ml/predict"))
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException exception) {
            throw new BusinessException(federatedMode
                    ? "联邦模型预测失败，请确认联邦模型已训练且 Python 模型服务正在运行"
                    : "模型预测失败，请确认 Python 模型服务正在运行");
        }
        if (prediction == null || prediction.get("transitionScore") == null) {
            throw new BusinessException("模型未返回有效评分");
        }
        // 只回写评分，不修改目标变量；授信动作仍由金融机构后续确认触发。
        BigDecimal transitionScore = new BigDecimal(prediction.get("transitionScore").toString());
        int affectedRows = dataMapper.updateTransitionScore(
                industry.pendingTableName(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName(),
                transitionScore
        );
        if (affectedRows == 0) {
            throw new BusinessException("待评估企业数据不存在或无权更新评分");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creditPending(Long id, String institutionId, String industryKey) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition industry = requireIndustry(industryKey);
        Map<String, Object> row = dataMapper.selectRowById(
                industry.pendingTableName(),
                industry.pendingSelectColumns(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (row == null) {
            throw new BusinessException("待评估企业数据不存在或无权授信");
        }
        if (row.get("transitionScore") == null) {
            throw new BusinessException("请先完成模型评估，再进行授信");
        }
        // 授信采用“先复制到历史表，再删除待评估表”的归档策略，
        // 整个过程放在事务里，避免只成功一半。
        row.put("verifiedTransitionPerformance", 1);
        Map<String, Object> params = buildWriteParams(industry, institution, row, false, true);
        List<ColumnDefinition> insertColumns = industry.insertColumns();
        String insertColumnSql = insertColumns.stream()
                .map(column -> "`" + column.column() + "`")
                .collect(Collectors.joining(", "));
        String valuePlaceholderSql = insertColumns.stream()
                .map(column -> "#{params." + column.prop() + "}")
                .collect(Collectors.joining(", "));
        dataMapper.insertRow(industry.tableName(), insertColumnSql, valuePlaceholderSql, params);
        int deletedRows = dataMapper.deleteRow(
                industry.pendingTableName(),
                id,
                institution.getInstitutionCode(),
                institution.getInstitutionName()
        );
        if (deletedRows == 0) {
            throw new BusinessException("待评估企业数据不存在或无权授信");
        }
    }

    @Override
    public List<TransitionScoreTrendVO> transitionScoreTrend(String institutionId, String industryKey, String companyCode) {
        FinancialInstitution institution = requireInstitution(institutionId);
        IndustryDefinition industry = requireIndustry(industryKey);
        if (!StringUtils.hasText(companyCode)) {
            throw new BusinessException("企业编码不能为空");
        }
        String sql = """
                select dataYear, transitionScore
                from (
                    select t.data_year as dataYear, t.transition_score as transitionScore
                      from %s t
                     where (
                               t.institution_code = ?
                               or (coalesce(t.institution_code, '') = '' and t.institution_name = ?)
                           )
                       and t.transition_score is not null
                       and exists (
                           select 1
                             from company c
                            where c.company_code = ?
                              and c.company_name = t.company_name
                              and (
                                  c.stock_code = t.stock_code
                                  or (c.stock_code is null and t.stock_code is null)
                                  or (c.stock_code = '-' and t.stock_code is null)
                                  or (c.stock_code = '-' and t.stock_code = '')
                                  or (c.stock_code is null and t.stock_code = '-')
                                  or (c.stock_code = '' and t.stock_code = '-')
                              )
                       )
                    order by t.data_year desc
                    limit 3
                ) latest
                order by dataYear
                """.formatted(industry.tableName());
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    TransitionScoreTrendVO vo = new TransitionScoreTrendVO();
                    vo.setDataYear(rs.getInt("dataYear"));
                    vo.setTransitionScore(rs.getBigDecimal("transitionScore"));
                    return vo;
                },
                institution.getInstitutionCode(),
                institution.getInstitutionName(),
                companyCode
        );
    }

    /**
     * 根据列白名单构造写入参数，并补齐当前金融机构与行业信息。
     */
    private Map<String, Object> buildWriteParams(IndustryDefinition industry,
                                                 FinancialInstitution institution,
                                                 Map<String, Object> requestData,
                                                 boolean creating,
                                                 boolean requireTargetValues) {
        if (requestData == null) {
            throw new BusinessException("企业评估数据不能为空");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("institutionCode", institution.getInstitutionCode());
        params.put("institutionName", institution.getInstitutionName());
        params.put("industryName", industry.name());
        for (ColumnDefinition column : industry.writableColumns()) {
            params.put(column.prop(), normalizeValue(column, requestData.get(column.prop())));
        }
        // 页面只接收企业编码；名称从企业主表解析，用于兼容行业表内部关联。
        String companyCode = requestData.get("companyCode") == null
                ? "" : requestData.get("companyCode").toString().trim();
        if (!StringUtils.hasText(companyCode)) {
            throw new BusinessException("企业编码不能为空");
        }
        List<String> companyNames = jdbcTemplate.queryForList(
                "select company_name from company where company_code = ?", String.class, companyCode);
        if (companyNames.size() != 1) {
            throw new BusinessException("企业编码不存在或不唯一，请核对企业编码");
        }
        params.put("companyName", companyNames.get(0));
        validateRequiredWriteFields(params, creating, requireTargetValues);
        return params;
    }

    /**
     * 校验新增/修改时必须填写的核心字段。
     */
    private void validateRequiredWriteFields(Map<String, Object> params, boolean creating, boolean requireTargetValues) {
        if (!StringUtils.hasText((String) params.get("companyName"))) {
            throw new BusinessException("企业名称不能为空");
        }
        String companyType = (String) params.get("companyType");
        if (!StringUtils.hasText(companyType)) {
            throw new BusinessException("企业类型不能为空");
        }
        if ("上市企业".equals(companyType)) {
            if (!StringUtils.hasText((String) params.get("stockCode"))) {
                throw new BusinessException("上市企业必须填写股票代码");
            }
        } else if ("非上市企业".equals(companyType)) {
            params.put("stockCode", null);
        } else {
            throw new BusinessException("企业类型只能选择上市企业或非上市企业");
        }
        if (params.get("dataYear") == null) {
            throw new BusinessException("年份不能为空");
        }
        if (requireTargetValues) {
            if (params.get("verifiedTransitionPerformance") == null) {
                throw new BusinessException("经核验转型绩效不能为空");
            }
        }
    }

    /**
     * 按列类型把前端传入值转换成数据库友好的值。
     */
    private Object normalizeValue(ColumnDefinition column, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String textValue && !StringUtils.hasText(textValue)) {
            return null;
        }
        if ("companyType".equals(column.prop())) {
            String normalized = value.toString().trim();
            if ("上市公司".equals(normalized)) {
                return "上市企业";
            }
            if ("非上市公司".equals(normalized)) {
                return "非上市企业";
            }
            return normalized;
        }
        if ("boolean".equals(column.type())) {
            return normalizeBoolean(value);
        }
        if ("number".equals(column.type())) {
            return normalizeNumber(column, value);
        }
        return value.toString().trim();
    }

    /**
     * 将布尔型展示值转换为 0/1。
     */
    private Integer normalizeBoolean(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue ? 1 : 0;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        String textValue = value.toString().trim();
        if ("是".equals(textValue) || "1".equals(textValue) || "true".equalsIgnoreCase(textValue)) {
            return 1;
        }
        if ("否".equals(textValue) || "0".equals(textValue) || "false".equalsIgnoreCase(textValue)) {
            return 0;
        }
        throw new BusinessException(columnValueError("布尔字段", value));
    }

    /**
     * 将数字型展示值转换为 Integer 或 BigDecimal。
     */
    private Object normalizeNumber(ColumnDefinition column, Object value) {
        try {
            if ("dataYear".equals(column.prop())) {
                if (value instanceof Number numberValue) {
                    return numberValue.intValue();
                }
                return Integer.parseInt(value.toString().trim());
            }
            if (value instanceof Number numberValue) {
                return new BigDecimal(numberValue.toString());
            }
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(columnValueError(column.label(), value));
        }
    }

    private String columnValueError(String label, Object value) {
        return label + "格式不正确：" + value;
    }

    /**
     * 校验金融机构编码存在，避免前端传入无效机构后看到其他机构数据。
     */
    private FinancialInstitution requireInstitution(String institutionId) {
        FinancialInstitution institution = institutionMapper.selectById(institutionId);
        if (institution == null) {
            throw new BusinessException("金融机构不存在，请重新登录");
        }
        return institution;
    }

    /**
     * 校验行业键在后端白名单内，避免动态表名被外部输入控制。
     */
    private IndustryDefinition requireIndustry(String industryKey) {
        IndustryDefinition industry = INDUSTRIES.get(industryKey);
        if (industry == null) {
            throw new BusinessException("行业不存在");
        }
        return industry;
    }

    private static Map<String, IndustryDefinition> createIndustryDefinitions() {
        Map<String, IndustryDefinition> industries = new LinkedHashMap<>();
        addIndustry(industries, "nonferrous", "有色金属行业", "nonferrous_metal_company_data", List.of(
                number("electrolyticAluminumRenewablePowerRatio", "electrolytic_aluminum_renewable_power_ratio", "电解铝绿电比例(%)", 190, null),
                number("electrolyticAluminumDcPowerConsumption", "electrolytic_aluminum_dc_power_consumption", "电解铝直流电耗", 190, null),
                number("recycledMetalSupplyRatio", "recycled_metal_supply_ratio", "再生金属供应占比(%)", 180, null),
                number("recycledAluminumOutput", "recycled_aluminum_output", "再生铝产量(万吨)", 170, null),
                number("aluminumCarbonEmissionIntensity", "aluminum_carbon_emission_intensity", "吨铝碳排放强度", 180, null),
                number("moltenAluminumDirectAlloyingRatio", "molten_aluminum_direct_alloying_ratio", "铝水直接合金化比例(%)", 190, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "power", "电力行业", "power_company_data", List.of(
                number("cleanEnergyInstalledCapacityRatio", "clean_energy_installed_capacity_ratio", "清洁能源装机占比(%)", 190, null),
                number("cleanEnergyGenerationRatio", "clean_energy_generation_ratio", "清洁能源发电量占比(%)", 200, null),
                number("powerSupplyStandardCoalConsumption", "power_supply_standard_coal_consumption", "供电标准煤耗", 170, null),
                number("powerGenerationCarbonIntensity", "power_generation_carbon_intensity", "发电碳排放强度", 170, null),
                number("coalPowerFlexibilityRetrofitRatio", "coal_power_flexibility_retrofit_ratio", "煤电灵活性改造比例(%)", 200, null),
                number("minimumTechnicalOutputPeakDepth", "minimum_technical_output_peak_depth", "最小技术出力调峰深度(%)", 210, null),
                number("renewableEnergyInvestment", "renewable_energy_investment", "可再生能源投资额(亿元)", 200, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "steel", "钢铁行业", "steel_company_data", List.of(
                number("blastFurnaceEnergyConsumption", "blast_furnace_energy_consumption", "高炉工序能耗", 160, null),
                number("converterEnergyConsumption", "converter_energy_consumption", "转炉工序能耗", 160, null),
                number("electricArcFurnaceEnergyConsumption", "electric_arc_furnace_energy_consumption", "电弧炉炼钢能耗", 170, null),
                number("steelCarbonEmissionIntensity", "steel_carbon_emission_intensity", "吨钢碳排放强度", 170, null),
                number("electricFurnaceShortProcessRatio", "electric_furnace_short_process_ratio", "电炉短流程占比(%)", 180, null),
                number("scrapSteelUse", "scrap_steel_use", "废钢利用量", 140, null),
                number("byproductGasUtilizationRatio", "byproduct_gas_utilization_ratio", "煤气利用率(%)", 150, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "aviation", "航空机场行业", "aviation_airport_company_data", List.of(
                text("subIndustryType", "sub_industry_type", "细分类型", 130, null),
                number("aviationFuelConsumptionPerTonKm", "aviation_fuel_consumption_per_ton_km", "吨公里油耗", 150, null),
                number("aviationCo2EmissionPerTonKm", "aviation_co2_emission_per_ton_km", "吨公里CO2排放", 170, null),
                number("co2EmissionPerRpk", "co2_emission_per_rpk", "RPK碳排放", 150, null),
                number("safUsage", "saf_usage", "SAF使用量(t)", 150, null),
                number("passengerThroughputEnergyConsumption", "passenger_throughput_energy_consumption", "旅客吞吐量能耗", 180, null),
                number("passengerThroughputCo2Emission", "passenger_throughput_co2_emission", "旅客吞吐量CO2排放", 190, null),
                number("renewableEnergyConsumptionRatio", "renewable_energy_consumption_ratio", "可再生能源消费占比(%)", 200, null),
                number("groundVehicleElectrificationRatio", "ground_vehicle_electrification_ratio", "地面车辆电动化比例(%)", 210, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "chemical", "基础化工行业", "basic_chemical_company_data", List.of(
                number("syntheticAmmoniaEnergyConsumption", "synthetic_ammonia_energy_consumption", "合成氨综合能耗", 170, null),
                number("ethyleneNaphthaCrackingEnergyConsumption", "ethylene_naphtha_cracking_energy_consumption", "乙烯裂解综合能耗", 190, null),
                number("methanolEnergyConsumption", "methanol_energy_consumption", "甲醇综合能耗", 160, null),
                number("productCarbonEmissionIntensity", "product_carbon_emission_intensity", "产品碳排放强度", 170, null),
                number("greenPowerUseRatio", "green_power_use_ratio", "绿色电力使用比例(%)", 190, null),
                number("greenHydrogenBiomassNonFossilFeedstockRatio", "green_hydrogen_biomass_non_fossil_feedstock_ratio", "非化石原料使用比例(%)", 210, null),
                number("industrialWaterReuseRatio", "industrial_water_reuse_ratio", "工业水重复利用率(%)", 190, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "building", "建材行业", "building_material_company_data", List.of(
                number("clinkerEnergyConsumption", "clinker_energy_consumption", "水泥熟料综合能耗", 180, null),
                number("flatGlassEnergyConsumption", "flat_glass_energy_consumption", "平板玻璃综合能耗", 180, null),
                number("productCarbonEmissionIntensity", "product_carbon_emission_intensity", "产品碳排放强度", 170, null),
                number("alternativeFuelThermalSubstitutionRatio", "alternative_fuel_thermal_substitution_ratio", "替代燃料热量替代率(%)", 220, null),
                number("alternativeRawMaterialUseRatio", "alternative_raw_material_use_ratio", "替代原料使用比例(%)", 200, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "coal", "煤炭行业", "coal_company_data", List.of(
                number("coalMineGasExtractionUtilizationRatio", "coal_mine_gas_extraction_utilization_ratio", "瓦斯抽采利用率(%)", 180, null),
                number("rawCoalEnergyConsumption", "raw_coal_energy_consumption", "原煤综合能耗", 150, null),
                number("mineWaterReuseRatio", "mine_water_reuse_ratio", "矿井水利用率(%)", 160, null),
                number("gangueFlyAshReuseRatio", "gangue_fly_ash_reuse_ratio", "煤矸石粉煤灰利用率(%)", 210, null),
                number("nonCoalRevenueRatio", "non_coal_revenue_ratio", "非煤收入占比(%)", 160, null),
                number("newEnergyInstalledCapacity", "new_energy_installed_capacity", "新能源装机容量(MW)", 190, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        addIndustry(industries, "petrochemical", "石油石化行业", "petrochemical_company_data", List.of(
                number("refiningEnergyConsumptionByEnergyFactor", "refining_energy_consumption_by_energy_factor", "炼油综合能耗", 170, null),
                number("refiningCarbonEmissionIntensity", "refining_carbon_emission_intensity", "炼油碳排放强度", 170, null),
                number("methaneEmissionIntensity", "methane_emission_intensity", "甲烷排放强度", 150, null),
                number("chemicalProductYieldRatio", "chemical_product_yield_ratio", "化工品收率占比(%)", 180, null),
                number("cleanEnergyUseRatio", "clean_energy_use_ratio", "清洁能源使用比例(%)", 190, null),
                number("sustainableAviationFuelBiofuelOutputSales", "sustainable_aviation_fuel_biofuel_output_sales", "可持续航空燃料产销量", 210, null),
                text("environmentalPerformanceRating", "environmental_performance_rating", "环保绩效评级", 140, null)
        ));
        return industries;
    }

    private static void addIndustry(Map<String, IndustryDefinition> industries,
                                    String key,
                                    String name,
                                    String tableName,
                                    List<ColumnDefinition> industryColumns) {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(text("id", "id", "ID", 90, "left"));
        columns.addAll(COMMON_COLUMNS);
        columns.addAll(industryColumns);
        columns.addAll(RESULT_COLUMNS);
        List<ColumnDefinition> insertColumns = new ArrayList<>();
        insertColumns.add(text("institutionCode", "institution_code", "金融机构编码", 150, null));
        insertColumns.add(text("institutionName", "institution_name", "金融机构名称", 150, null));
        insertColumns.add(text("industryName", "industry_name", "行业", 140, null));
        insertColumns.addAll(columns.stream()
                .filter(ColumnDefinition::writable)
                .collect(Collectors.toList()));
        industries.put(key, new IndustryDefinition(key, name, tableName, columns, insertColumns));
    }

    private static ColumnDefinition text(String prop, String column, String label, Integer width, String fixed) {
        return new ColumnDefinition(prop, column, label, width, "text", fixed);
    }

    private static ColumnDefinition number(String prop, String column, String label, Integer width, String fixed) {
        return new ColumnDefinition(prop, column, label, width, "number", fixed);
    }

    private static ColumnDefinition bool(String prop, String column, String label, Integer width) {
        return new ColumnDefinition(prop, column, label, width, "boolean", null);
    }

    /**
     * 行业定义，包含表名与列白名单。
     */
    private record IndustryDefinition(String key,
                                      String name,
                                      String tableName,
                                      List<ColumnDefinition> columns,
                                      List<ColumnDefinition> insertColumns) {

        private String selectColumns() {
            return columns.stream()
                    .map(ColumnDefinition::selectExpression)
                    .collect(Collectors.joining(", "));
        }

        private String pendingTableName() {
            return tableName.replace("_company_data", "_pending_company_data");
        }

        private List<ColumnDefinition> pendingColumns() {
            return columns.stream()
                    .filter(column -> !column.targetValue())
                    .collect(Collectors.toList());
        }

        private String pendingSelectColumns() {
            return pendingColumns().stream()
                    .map(ColumnDefinition::selectExpression)
                    .collect(Collectors.joining(", "));
        }

        private List<ColumnDefinition> writableColumns() {
            return columns.stream()
                    .filter(ColumnDefinition::writable)
                    .collect(Collectors.toList());
        }

        private List<ColumnDefinition> pendingWritableColumns() {
            return pendingColumns().stream()
                    .filter(ColumnDefinition::writable)
                    .collect(Collectors.toList());
        }

        private List<ColumnDefinition> pendingInsertColumns() {
            return insertColumns.stream()
                    .filter(column -> !column.targetValue())
                    .collect(Collectors.toList());
        }
    }

    /**
     * 数据库列与前端列之间的映射。
     */
    private record ColumnDefinition(String prop,
                                    String column,
                                    String label,
                                    Integer width,
                                    String type,
                                    String fixed) {

        private ColumnMetaVO toVO() {
            return new ColumnMetaVO(prop, label, width, type, fixed);
        }

        private boolean writable() {
            return !"id".equals(prop)
                    && !"institutionCode".equals(prop)
                    && !"institutionName".equals(prop)
                    && !"companyCode".equals(prop)
                    && !"industryName".equals(prop);
        }

        private boolean targetValue() {
            return "verifiedTransitionPerformance".equals(prop);
        }

        private String selectExpression() {
            if ("companyCode".equals(prop)) {
                return com.transfinance.support.CompanyIdentitySql.CODE + " as `companyCode`";
            }
            return "t.`" + column + "` as `" + prop + "`";
        }
    }
}
