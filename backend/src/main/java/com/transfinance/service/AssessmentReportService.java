package com.transfinance.service;

import com.transfinance.vo.AssessmentReportVO;

/**
 * 转型金融评估报告服务。
 */
public interface AssessmentReportService {

    /**
     * 基于待评估企业数据生成评分报告。
     *
     * @param institutionId 金融机构编码
     * @param industryKey 行业键
     * @param id 待评估数据主键ID
     * @param modelMode 模型来源
     * @return 评估报告
     */
    AssessmentReportVO buildPendingReport(String institutionId, String industryKey, Long id, String modelMode);
}
