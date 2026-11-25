package com.transfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.common.PageResult;
import com.transfinance.dto.CompanyMissingDataSaveRequest;
import com.transfinance.entity.Company;
import com.transfinance.entity.FinancialInstitution;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.CompanyMapper;
import com.transfinance.mapper.CompanyMissingDataMapper;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.service.CompanyMissingDataService;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.support.IndustryDataCatalog.FieldMeta;
import com.transfinance.support.IndustryDataCatalog.IndustryMeta;
import com.transfinance.util.CompanyCodeGenerator;
import com.transfinance.vo.CompanyMissingReminderVO;
import com.transfinance.vo.FinancialDataChangeNoticeVO;
import com.transfinance.vo.MissingColumnVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 企业缺失数据提醒与补全服务实现。
 */
@Service
public class CompanyMissingDataServiceImpl implements CompanyMissingDataService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final TypeReference<List<MissingColumnVO>> MISSING_COLUMN_LIST = new TypeReference<>() {
    };

    private final CompanyMissingDataMapper missingDataMapper;
    private final CompanyMapper companyMapper;
    private final FinancialInstitutionMapper financialInstitutionMapper;
    private final ObjectMapper objectMapper;

    public CompanyMissingDataServiceImpl(CompanyMissingDataMapper missingDataMapper,
                                         CompanyMapper companyMapper,
                                         FinancialInstitutionMapper financialInstitutionMapper,
                                         ObjectMapper objectMapper) {
        this.missingDataMapper = missingDataMapper;
        this.companyMapper = companyMapper;
        this.financialInstitutionMapper = financialInstitutionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanAndNotifyMissingData(String institutionCode) {
        return scanMissingTrainingData(institutionCode, true);
    }

    @Override
    public int countMissingTrainingData(String institutionCode) {
        return scanMissingTrainingData(institutionCode, false);
    }

    private int scanMissingTrainingData(String institutionCode, boolean createReminder) {
        FinancialInstitution institution = financialInstitutionMapper.selectById(institutionCode);
        if (institution == null) {
            throw new BusinessException("金融机构不存在，请重新登录");
        }
        int missingRowCount = 0;
        for (IndustryMeta industry : IndustryDataCatalog.industries().values()) {
            Integer maxYear = missingDataMapper.selectMaxYear(
                    industry.tableName(),
                    institutionCode,
                    institution.getInstitutionName()
            );
            if (maxYear == null) {
                continue;
            }
            List<Map<String, Object>> rows = missingDataMapper.selectTrainingRows(
                    industry.tableName(),
                    selectColumns(industry),
                    institutionCode,
                    institution.getInstitutionName(),
                    maxYear
            );
            for (Map<String, Object> row : rows) {
                List<MissingColumnVO> missingColumns = missingColumns(industry, row);
                if (missingColumns.isEmpty()) {
                    continue;
                }
                if (createReminder) {
                    Company company = ensureCompany(row, industry);
                    missingDataMapper.upsertReminder(
                            company.getCompanyCode(),
                            asText(row.get("companyName")),
                            StringUtils.hasText(asText(row.get("institutionCode")))
                                    ? asText(row.get("institutionCode"))
                                    : institutionCode,
                            StringUtils.hasText(asText(row.get("institutionName")))
                                    ? asText(row.get("institutionName"))
                                    : institution.getInstitutionName(),
                            industry.key(),
                            industry.name(),
                            industry.tableName(),
                            asLong(row.get("id")),
                            asInteger(row.get("dataYear")),
                            toJson(missingColumns)
                    );
                }
                missingRowCount++;
            }
        }
        return missingRowCount;
    }

    @Override
    public PageResult<CompanyMissingReminderVO> companyReminders(String companyCode, Long page, Long size) {
        long currentPage = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        long offset = (currentPage - 1) * pageSize;
        List<CompanyMissingReminderVO> rows = missingDataMapper.selectCompanyReminderPage(companyCode, offset, pageSize);
        Long total = missingDataMapper.countCompanyReminders(companyCode);
        return new PageResult<>(rows, total == null ? 0 : total, currentPage, pageSize);
    }

    @Override
    public Map<String, Object> reminderDetail(String companyCode, Long reminderId) {
        CompanyMissingReminderVO reminder = requireReminder(companyCode, reminderId);
        IndustryMeta industry = IndustryDataCatalog.requireIndustry(reminder.getIndustryKey());
        Map<String, Object> row = missingDataMapper.selectSourceRow(
                industry.tableName(),
                selectColumns(industry),
                reminder.getSourceRowId()
        );
        if (row == null) {
            throw new BusinessException("原始数据不存在");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reminder", reminder);
        response.put("row", row);
        response.put("missingColumns", parseMissingColumns(reminder.getMissingColumnsJson()));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMissingValues(Long reminderId, CompanyMissingDataSaveRequest request) {
        CompanyMissingReminderVO reminder = requireReminder(request.getCompanyCode(), reminderId);
        IndustryMeta industry = IndustryDataCatalog.requireIndustry(reminder.getIndustryKey());
        List<MissingColumnVO> missingColumns = parseMissingColumns(reminder.getMissingColumnsJson());
        Set<String> allowedProps = missingColumns.stream().map(MissingColumnVO::getProp).collect(Collectors.toSet());
        Map<String, Object> sourceRow = missingDataMapper.selectSourceRow(
                industry.tableName(),
                selectColumns(industry),
                reminder.getSourceRowId()
        );
        if (sourceRow == null) {
            throw new BusinessException("原始数据不存在");
        }

        Map<String, Object> params = new HashMap<>();
        List<String> setItems = new ArrayList<>();
        List<MissingColumnVO> changedColumns = new ArrayList<>();
        for (Map.Entry<String, Object> entry : request.getValues().entrySet()) {
            if (!allowedProps.contains(entry.getKey())) {
                throw new BusinessException("只能补全本次提醒中的缺失字段");
            }
            FieldMeta field = IndustryDataCatalog.requireField(industry.key(), entry.getKey());
            Object normalizedValue = normalizeValue(field, entry.getValue());
            if (normalizedValue == null) {
                throw new BusinessException(field.label() + "不能为空");
            }
            params.put(field.prop(), normalizedValue);
            setItems.add("`" + field.column() + "` = #{params." + field.prop() + "}");
            MissingColumnVO changed = new MissingColumnVO(field.prop(), field.column(), field.label(), field.type());
            changed.setOldValue(sourceRow.get(field.prop()));
            changed.setNewValue(normalizedValue);
            changedColumns.add(changed);
        }
        if (setItems.isEmpty()) {
            throw new BusinessException("请至少补全一个字段");
        }
        int affectedRows = missingDataMapper.updateSourceRow(
                industry.tableName(),
                String.join(", ", setItems),
                reminder.getSourceRowId(),
                params
        );
        if (affectedRows == 0) {
            throw new BusinessException("原始数据更新失败");
        }
        missingDataMapper.updateChangedColumns(reminderId, request.getCompanyCode(), toJson(changedColumns));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReminder(String companyCode, Long reminderId) {
        CompanyMissingReminderVO reminder = requireReminder(companyCode, reminderId);
        if (!StringUtils.hasText(reminder.getChangedColumnsJson())) {
            throw new BusinessException("请先填补缺失字段后再提交");
        }
        missingDataMapper.insertChangeNotice(
                reminder.getInstitutionCode(),
                reminder.getCompanyCode(),
                reminder.getCompanyName(),
                reminder.getIndustryKey(),
                reminder.getIndustryName(),
                reminder.getSourceTable(),
                reminder.getSourceRowId(),
                reminder.getDataYear(),
                reminder.getChangedColumnsJson()
        );
        int affectedRows = missingDataMapper.submitReminder(reminderId, companyCode);
        if (affectedRows == 0) {
            throw new BusinessException("提醒不存在或已提交");
        }
    }

    @Override
    public PageResult<FinancialDataChangeNoticeVO> financialNotices(String institutionCode,
                                                                    String keyword,
                                                                    String industryKey,
                                                                    Integer dataYear,
                                                                    String status,
                                                                    Long page,
                                                                    Long size) {
        if (financialInstitutionMapper.selectById(institutionCode) == null) {
            throw new BusinessException("金融机构不存在，请重新登录");
        }
        if (StringUtils.hasText(status) && !"PENDING".equals(status) && !"SUBMITTED".equals(status)) {
            throw new BusinessException("通知状态不合法");
        }
        if (StringUtils.hasText(industryKey)) {
            IndustryDataCatalog.requireIndustry(industryKey);
        }
        long currentPage = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        long offset = (currentPage - 1) * pageSize;
        List<FinancialDataChangeNoticeVO> rows = missingDataMapper.selectFinancialNoticePage(
                institutionCode,
                keyword,
                industryKey,
                dataYear,
                status,
                offset,
                pageSize
        );
        Long total = missingDataMapper.countFinancialNotices(institutionCode, keyword, industryKey, dataYear, status);
        return new PageResult<>(rows, total == null ? 0 : total, currentPage, pageSize);
    }

    private CompanyMissingReminderVO requireReminder(String companyCode, Long reminderId) {
        CompanyMissingReminderVO reminder = missingDataMapper.selectReminderForCompany(reminderId, companyCode);
        if (reminder == null) {
            throw new BusinessException("缺失数据提醒不存在");
        }
        return reminder;
    }

    private Company ensureCompany(Map<String, Object> row, IndustryMeta industry) {
        String companyName = asText(row.get("companyName"));
        String stockCode = asText(row.get("stockCode"));
        Company existing = com.transfinance.support.CompanyIdentityResolver.find(
                companyMapper, companyName, industry.key(), stockCode);
        if (existing != null) {
            return existing;
        }
        Company company = new Company();
        company.setCompanyCode(generateUniqueCompanyCode(companyName));
        company.setCompanyName(companyName);
        company.setCompanyType(asText(row.get("companyType")));
        company.setStockCode(StringUtils.hasText(stockCode) ? stockCode : "-");
        company.setIndustryKey(industry.key());
        company.setIndustryName(industry.name());
        companyMapper.insert(company);
        return company;
    }

    private String generateUniqueCompanyCode(String companyName) {
        String baseCode = CompanyCodeGenerator.initials(companyName);
        if (companyMapper.selectById(baseCode) == null) {
            return baseCode;
        }
        for (int i = 0; i < 20; i++) {
            String nextCode = baseCode + "_" + SECURE_RANDOM.nextInt(10000);
            if (companyMapper.selectById(nextCode) == null) {
                return nextCode;
            }
        }
        throw new BusinessException("企业编码生成失败，请稍后重试");
    }

    private List<MissingColumnVO> missingColumns(IndustryMeta industry, Map<String, Object> row) {
        List<MissingColumnVO> missingColumns = new ArrayList<>();
        for (FieldMeta field : industry.featureFields()) {
            Object value = row.get(field.prop());
            if (value == null || (value instanceof String text && !StringUtils.hasText(text))) {
                missingColumns.add(new MissingColumnVO(field.prop(), field.column(), field.label(), field.type()));
            }
        }
        return missingColumns;
    }

    private String selectColumns(IndustryMeta industry) {
        List<String> columns = new ArrayList<>();
        columns.add("`id` as `id`");
        columns.add("`institution_code` as `institutionCode`");
        columns.add("`institution_name` as `institutionName`");
        columns.add("`company_name` as `companyName`");
        columns.add("`stock_code` as `stockCode`");
        columns.add("`company_type` as `companyType`");
        columns.add("`data_year` as `dataYear`");
        for (FieldMeta field : industry.featureFields()) {
            columns.add("`" + field.column() + "` as `" + field.prop() + "`");
        }
        return String.join(", ", columns);
    }

    private List<MissingColumnVO> parseMissingColumns(String json) {
        try {
            return objectMapper.readValue(json, MISSING_COLUMN_LIST);
        } catch (Exception exception) {
            throw new BusinessException("缺失字段数据格式异常");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException("数据序列化失败");
        }
    }

    private Object normalizeValue(FieldMeta field, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text && !StringUtils.hasText(text)) {
            return null;
        }
        if ("boolean".equals(field.type())) {
            if (value instanceof Boolean boolValue) {
                return boolValue ? 1 : 0;
            }
            if (value instanceof Number numberValue) {
                return numberValue.intValue();
            }
            String text = value.toString().trim();
            if ("是".equals(text) || "1".equals(text) || "true".equalsIgnoreCase(text)) {
                return 1;
            }
            if ("否".equals(text) || "0".equals(text) || "false".equalsIgnoreCase(text)) {
                return 0;
            }
            throw new BusinessException(field.label() + "格式不正确");
        }
        if ("number".equals(field.type())) {
            if (value instanceof Number numberValue) {
                return new BigDecimal(numberValue.toString());
            }
            try {
                return new BigDecimal(value.toString().trim());
            } catch (NumberFormatException exception) {
                throw new BusinessException(field.label() + "必须是数字");
            }
        }
        return value.toString().trim();
    }

    private String asText(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private Long asLong(Object value) {
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        return Integer.valueOf(value.toString());
    }
}
