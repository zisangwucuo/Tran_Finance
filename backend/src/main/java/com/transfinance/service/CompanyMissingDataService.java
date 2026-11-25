package com.transfinance.service;

import com.transfinance.common.PageResult;
import com.transfinance.dto.CompanyMissingDataSaveRequest;
import com.transfinance.vo.CompanyMissingReminderVO;
import com.transfinance.vo.FinancialDataChangeNoticeVO;

import java.util.Map;

/**
 * 企业缺失数据提醒与补全服务。
 */
public interface CompanyMissingDataService {

    int scanAndNotifyMissingData(String institutionCode);

    int countMissingTrainingData(String institutionCode);

    PageResult<CompanyMissingReminderVO> companyReminders(String companyCode, Long page, Long size);

    Map<String, Object> reminderDetail(String companyCode, Long reminderId);

    void saveMissingValues(Long reminderId, CompanyMissingDataSaveRequest request);

    void submitReminder(String companyCode, Long reminderId);

    PageResult<FinancialDataChangeNoticeVO> financialNotices(String institutionCode,
                                                             String keyword,
                                                             String industryKey,
                                                             Integer dataYear,
                                                             String status,
                                                             Long page,
                                                             Long size);
}
