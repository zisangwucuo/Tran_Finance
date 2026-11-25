package com.transfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.transfinance.common.PageResult;
import com.transfinance.dto.CompanyDataQueryRequest;
import com.transfinance.entity.Company;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.CompanyDataMapper;
import com.transfinance.mapper.CompanyMapper;
import com.transfinance.service.CompanyDataService;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.support.IndustryDataCatalog.FieldMeta;
import com.transfinance.support.IndustryDataCatalog.IndustryMeta;
import com.transfinance.vo.CompanyAssessmentDetailVO;
import com.transfinance.vo.CompanyAssessmentPageResponse;
import com.transfinance.vo.CompanyAssessmentRecordVO;
import com.transfinance.vo.IndustryOptionVO;
import com.transfinance.vo.IndustryOptionsResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业端历史数据与待评估数据服务实现。
 */
@Service
public class CompanyDataServiceImpl implements CompanyDataService {

    private final CompanyDataMapper companyDataMapper;
    private final CompanyMapper companyMapper;

    public CompanyDataServiceImpl(CompanyDataMapper companyDataMapper,
                                  CompanyMapper companyMapper) {
        this.companyDataMapper = companyDataMapper;
        this.companyMapper = companyMapper;
    }

    @Override
    public CompanyAssessmentPageResponse page(CompanyDataQueryRequest request, boolean pending) {
        Company company = requireCompany(request.getCompanyCode());
        long currentPage = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        long pageSize = request.getSize() == null || request.getSize() < 1
                ? 10
                : Math.min(request.getSize(), 100);
        long offset = (currentPage - 1) * pageSize;

        // 这里把八个行业的历史表或待评估表拼成一个受控的 union all 视图，
        // 这样企业端只需要传公司编码，就能看见自己名下跨行业的数据。
        String unionSql = buildSummaryUnionSql(pending);
        List<Map<String, Object>> rows = companyDataMapper.selectCompanyDataPage(
                unionSql,
                company.getCompanyName(),
                company.getStockCode(),
                request.getIndustryKey(),
                request.getDataYear(),
                offset,
                pageSize
        );
        Long total = companyDataMapper.countCompanyData(
                unionSql,
                company.getCompanyName(),
                company.getStockCode(),
                request.getIndustryKey(),
                request.getDataYear()
        );

        List<CompanyAssessmentRecordVO> records = rows.stream()
                .map(row -> toRecord(row, request.getCompanyCode(), pending))
                .toList();

        CompanyAssessmentPageResponse response = new CompanyAssessmentPageResponse();
        response.setDatasetType(pending ? "pending" : "history");
        response.setCompanyCode(company.getCompanyCode());
        response.setCompanyName(company.getCompanyName());
        response.setPage(new PageResult<>(
                records,
                total == null ? 0 : total,
                currentPage,
                pageSize
        ));
        return response;
    }

    @Override
    public IndustryOptionsResponse options(String companyCode, String industryKey, boolean pending) {
        Company company = requireCompany(companyCode);
        String unionSql = buildSummaryUnionSql(pending);
        // 行业和年份筛选项都从企业自己拥有的数据里反推，不额外暴露其他企业的数据范围。
        List<Map<String, Object>> industryRows = companyDataMapper.selectCompanyIndustries(
                unionSql,
                company.getCompanyName(),
                company.getStockCode()
        );
        List<IndustryOptionVO> industries = industryRows.stream()
                .map(row -> new IndustryOptionVO(
                        asString(row.get("industryKey")),
                        asString(row.get("industryName"))
                ))
                .filter(item -> StringUtils.hasText(item.getKey()))
                .collect(Collectors.toList());

        IndustryOptionsResponse response = new IndustryOptionsResponse();
        response.setIndustries(industries);
        response.setYears(companyDataMapper.selectCompanyYears(
                unionSql,
                company.getCompanyName(),
                company.getStockCode(),
                industryKey
        ));
        return response;
    }

    @Override
    public CompanyAssessmentDetailVO detail(String companyCode,
                                            String industryKey,
                                            Long id,
                                            boolean pending) {
        Company company = requireCompany(companyCode);
        IndustryMeta industry = requireIndustry(industryKey);
        if (id == null) {
            throw new BusinessException("数据ID不能为空");
        }

        String tableName = pending ? industry.pendingTableName() : industry.tableName();
        Map<String, Object> row = companyDataMapper.selectCompanyDataDetail(
                tableName,
                buildDetailSelectColumns(industry, pending),
                id,
                company.getCompanyName(),
                company.getStockCode()
        );
        if (row == null) {
            throw new BusinessException("企业数据不存在或无权查看");
        }
        row.put("industryKey", industry.key());
        if (!StringUtils.hasText(asString(row.get("industryName")))) {
            row.put("industryName", industry.name());
        }

        // 详情页把数据库原始行拆成“概览 + 字段明细”两部分，方便前端做信息分组展示。
        CompanyAssessmentDetailVO response = new CompanyAssessmentDetailVO();
        response.setSummary(toRecord(row, companyCode, pending));
        response.setFields(buildDetailFields(row, industry, pending));
        return response;
    }

    /**
     * 以行业白名单拼接跨行业摘要查询 SQL。
     */
    private String buildSummaryUnionSql(boolean pending) {
        // 只允许从白名单行业中取表名，避免前端输入直接进入动态 SQL。
        return IndustryDataCatalog.industries().values().stream()
                .map(industry -> {
                    String tableName = pending ? industry.pendingTableName() : industry.tableName();
                    String verifiedTransitionPerformanceExpression = pending
                            ? "null"
                            : "t.`verified_transition_performance`";
                    String industryNameExpression = "coalesce(t.`industry_name`, '"
                            + escapeSqlLiteral(industry.name()) + "')";
                    return """
                            select t.`id` as `id`,
                                   t.`institution_code` as `institutionCode`,
                                   t.`institution_name` as `institutionName`,
                                   %s as `industryName`,
                                   '%s' as `industryKey`,
                                   t.`company_name` as `companyName`,
                                   t.`stock_code` as `stockCode`,
                                   t.`company_type` as `companyType`,
                                   t.`data_year` as `dataYear`,
                                   t.`transition_score` as `transitionScore`,
                                   %s as `verifiedTransitionPerformance`
                              from `%s` t
                            """.formatted(
                            industryNameExpression,
                            escapeSqlLiteral(industry.key()),
                            verifiedTransitionPerformanceExpression,
                            tableName
                    );
                })
                .collect(Collectors.joining(" union all "));
    }

    /**
     * 以行业白名单拼接单条详情查询字段。
     */
    private String buildDetailSelectColumns(IndustryMeta industry, boolean pending) {
        List<String> expressions = new ArrayList<>();
        expressions.add("t.`id` as `id`");
        expressions.add("t.`institution_code` as `institutionCode`");
        expressions.add("t.`institution_name` as `institutionName`");
        expressions.add("t.`industry_name` as `industryName`");
        expressions.add("t.`company_name` as `companyName`");
        expressions.add("t.`stock_code` as `stockCode`");
        expressions.add("t.`company_type` as `companyType`");
        expressions.add("t.`data_year` as `dataYear`");
        for (FieldMeta field : industry.featureFields()) {
            expressions.add("t.`" + field.column() + "` as `" + field.prop() + "`");
        }
        expressions.add("t.`transition_score` as `transitionScore`");
        expressions.add(pending
                ? "null as `verifiedTransitionPerformance`"
                : "t.`verified_transition_performance` as `verifiedTransitionPerformance`");
        return String.join(", ", expressions);
    }

    private List<CompanyAssessmentDetailVO.FieldValue> buildDetailFields(Map<String, Object> row,
                                                                           IndustryMeta industry,
                                                                           boolean pending) {
        List<CompanyAssessmentDetailVO.FieldValue> fields = new ArrayList<>();
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "institutionName", "金融机构", "text", row.get("institutionName")
        ));
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "industryName", "行业", "text", row.get("industryName")
        ));
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "companyName", "企业名称", "text", row.get("companyName")
        ));
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "companyType", "企业类型", "text", row.get("companyType")
        ));
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "stockCode", "股票代码", "text", row.get("stockCode")
        ));
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "dataYear", "年份", "number", row.get("dataYear")
        ));
        for (FieldMeta field : industry.featureFields()) {
            fields.add(new CompanyAssessmentDetailVO.FieldValue(
                    field.prop(),
                    field.label(),
                    field.type(),
                    row.get(field.prop())
            ));
        }
        fields.add(new CompanyAssessmentDetailVO.FieldValue(
                "transitionScore", "转型综合评分", "number", row.get("transitionScore")
        ));
        if (!pending) {
            fields.add(new CompanyAssessmentDetailVO.FieldValue(
                    "verifiedTransitionPerformance", "经核验转型绩效", "boolean", row.get("verifiedTransitionPerformance")
            ));
        }
        return fields;
    }

    private CompanyAssessmentRecordVO toRecord(Map<String, Object> row,
                                               String companyCode,
                                               boolean pending) {
        String industryKey = asString(row.get("industryKey"));
        IndustryMeta industry = requireIndustry(industryKey);
        BigDecimal score = asBigDecimal(row.get("transitionScore"));
        Integer verifiedTransitionPerformance = asInteger(row.get("verifiedTransitionPerformance"));
        String rating = asString(row.get("environmentalPerformanceRating"));

        CompanyAssessmentRecordVO record = new CompanyAssessmentRecordVO();
        record.setId(asLong(row.get("id")));
        record.setIndustryKey(industry.key());
        record.setIndustryName(StringUtils.hasText(asString(row.get("industryName")))
                ? asString(row.get("industryName"))
                : industry.name());
        record.setInstitutionCode(asString(row.get("institutionCode")));
        record.setInstitutionName(StringUtils.hasText(asString(row.get("institutionName")))
                ? asString(row.get("institutionName"))
                : "未标明金融机构");
        record.setCompanyCode(companyCode);
        record.setCompanyName(asString(row.get("companyName")));
        record.setStockCode(asString(row.get("stockCode")));
        record.setCompanyType(asString(row.get("companyType")));
        record.setDataYear(asInteger(row.get("dataYear")));
        record.setTransitionScore(score == null ? null : score.setScale(2, RoundingMode.HALF_UP));
        record.setVerifiedTransitionPerformance(verifiedTransitionPerformance);

        // 同一条记录在企业端需要显示成“状态、结果标签、建议”三类信息，
        // 所以这里统一把评分和授信结果翻译成业务语言。
        AssessmentAdvice advice = buildAssessmentAdvice(score, verifiedTransitionPerformance, rating, pending);
        record.setAssessmentStatus(advice.status());
        record.setResultLabel(advice.resultLabel());
        record.setAssessmentAdvice(advice.summary());
        record.setSuggestions(advice.suggestions());
        return record;
    }

    /**
     * 生成企业可直接理解的评估状态和改进建议。
     */
    private AssessmentAdvice buildAssessmentAdvice(BigDecimal score,
                                                   Integer verifiedTransitionPerformance,
                                                   String rating,
                                                   boolean pending) {
        List<String> suggestions = new ArrayList<>();
        if (score == null) {
            if (pending) {
                suggestions.add("金融机构尚未完成本条数据评估，评估完成后本页面会自动更新结果。");
                return new AssessmentAdvice("待金融机构评估", "等待评估", suggestions.get(0), suggestions);
            }
            suggestions.add("该条历史记录暂未生成转型综合评分，可补充后续评估结果。");
            return new AssessmentAdvice("暂无评分", "暂无结果", suggestions.get(0), suggestions);
        }

        if (pending) {
            // 待评估页只展示“等待授信”语境，因此建议内容会偏向继续补充和观察。
            if (score.compareTo(new BigDecimal("80")) >= 0) {
                suggestions.add("当前综合表现较好，建议保持现有转型投入并持续披露关键指标。");
            } else if (score.compareTo(new BigDecimal("70")) >= 0) {
                suggestions.add("当前综合表现良好，建议继续提升专项减排指标和转型计划执行力度。");
            } else if (score.compareTo(new BigDecimal("60")) >= 0) {
                suggestions.add("当前综合表现一般，建议优先改善低分指标并完善转型实施路径。");
            } else {
                suggestions.add("当前综合评分偏低，建议尽快补强转型治理、减排投入和专项技术指标。");
            }
            String summary = "已生成转型综合评分，等待金融机构进一步授信。";
            appendRatingSuggestion(suggestions, rating);
            return new AssessmentAdvice("已评估，待授信", "评分已生成", summary, suggestions);
        }

        if (verifiedTransitionPerformance != null && verifiedTransitionPerformance == 1) {
            // 历史页里如果已经授信，就强调后续持续执行和定期更新。
            suggestions.add("该条历史数据已完成授信，建议按照转型计划持续执行并定期更新指标。");
        } else if (verifiedTransitionPerformance != null && verifiedTransitionPerformance == 0) {
            suggestions.add("该条历史数据未达到经核验转型绩效标准，建议针对低分指标制定改进措施。");
        } else {
            suggestions.add("该条历史数据已生成评分，暂未记录授信结果。");
        }
        appendScoreSuggestion(suggestions, score);
        appendRatingSuggestion(suggestions, rating);
        String resultLabel = verifiedTransitionPerformance != null && verifiedTransitionPerformance == 1
                ? "符合经核验转型绩效"
                : verifiedTransitionPerformance != null && verifiedTransitionPerformance == 0
                ? "不符合经核验转型绩效" : "已完成评估";
        return new AssessmentAdvice("已完成评估", resultLabel, suggestions.get(0), suggestions);
    }

    private void appendScoreSuggestion(List<String> suggestions, BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            suggestions.add("建议保持高分指标表现，同时持续关注指标波动，避免后续年度评分回落。");
        } else if (score.compareTo(new BigDecimal("70")) >= 0) {
            suggestions.add("建议重点提升行业专项指标，并完善转型目标、路径和信息披露的连续性。");
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            suggestions.add("建议优先整改得分较低的专项指标，补充量化目标、实施节点和佐证材料。");
        } else {
            suggestions.add("建议从转型治理、技术应用、能源结构和碳排放管理等方面制定专项整改计划。");
        }
    }

    private void appendRatingSuggestion(List<String> suggestions, String rating) {
        if (!StringUtils.hasText(rating)) {
            return;
        }
        if (rating.contains("C")) {
            suggestions.add("环保绩效评级偏低，建议优先完善环保合规管理并降低环境风险。");
        } else if (rating.contains("B")) {
            suggestions.add("环保绩效处于中等水平，建议持续提升污染防治和清洁能源使用表现。");
        } else if (rating.contains("A")) {
            suggestions.add("环保绩效表现较好，建议继续保持并形成可持续披露机制。");
        }
    }

    private Company requireCompany(String companyCode) {
        if (!StringUtils.hasText(companyCode)) {
            throw new BusinessException("企业编码不能为空");
        }
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getCompanyCode, companyCode)
                .last("limit 1"));
        if (company == null) {
            throw new BusinessException("企业不存在，请重新登录");
        }
        return company;
    }

    private IndustryMeta requireIndustry(String industryKey) {
        if (!StringUtils.hasText(industryKey)) {
            throw new BusinessException("行业不能为空");
        }
        return IndustryDataCatalog.requireIndustry(industryKey);
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String asString(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 评估结果摘要。
     */
    private record AssessmentAdvice(String status,
                                    String resultLabel,
                                    String summary,
                                    List<String> suggestions) {
    }
}
