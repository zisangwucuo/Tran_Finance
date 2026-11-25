package com.transfinance.service;

import com.transfinance.dto.CompanyDataQueryRequest;
import com.transfinance.vo.CompanyAssessmentDetailVO;
import com.transfinance.vo.CompanyAssessmentPageResponse;
import com.transfinance.vo.IndustryOptionsResponse;

/**
 * 企业端历史数据与待评估数据服务。
 */
public interface CompanyDataService {

    /**
     * 查询当前企业的历史数据或待评估数据。
     *
     * @param request 查询条件
     * @param pending 是否查询待评估数据
     * @return 分页结果
     */
    CompanyAssessmentPageResponse page(CompanyDataQueryRequest request, boolean pending);

    /**
     * 查询当前企业可用的行业和年份筛选项。
     *
     * @param companyCode 企业编码
     * @param pending 是否查询待评估数据筛选项
     * @return 筛选项
     */
    IndustryOptionsResponse options(String companyCode, String industryKey, boolean pending);

    /**
     * 查询企业某条历史数据或待评估数据的完整详情。
     *
     * @param companyCode 企业编码
     * @param industryKey 行业键
     * @param id 数据行ID
     * @param pending 是否查询待评估数据
     * @return 详情
     */
    CompanyAssessmentDetailVO detail(String companyCode, String industryKey, Long id, boolean pending);
}
