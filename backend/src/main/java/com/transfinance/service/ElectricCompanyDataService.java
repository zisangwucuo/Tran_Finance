package com.transfinance.service;

import com.transfinance.dto.ElectricCompanyDataQueryRequest;
import com.transfinance.dto.IndustryCompanyDataSaveRequest;
import com.transfinance.vo.IndustryCompanyDataPageResponse;
import com.transfinance.vo.IndustryOptionsResponse;
import com.transfinance.vo.TransitionScoreTrendVO;

import java.util.List;

/**
 * 企业评估数据服务。
 */
public interface ElectricCompanyDataService {

    /**
     * 分页查询企业评估数据。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IndustryCompanyDataPageResponse page(ElectricCompanyDataQueryRequest request);

    /**
     * 分页查询待评估企业数据。
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IndustryCompanyDataPageResponse pendingPage(ElectricCompanyDataQueryRequest request);

    /**
     * 查询当前金融机构可查看的行业和年份筛选项。
     *
     * @param institutionId 金融机构编码
     * @param industryKey 当前行业键
     * @return 筛选项
     */
    IndustryOptionsResponse options(String institutionId, String industryKey);

    /**
     * 查询当前金融机构可查看的待评估行业和年份筛选项。
     *
     * @param institutionId 金融机构编码
     * @param industryKey 当前行业键
     * @return 筛选项
     */
    IndustryOptionsResponse pendingOptions(String institutionId, String industryKey);

    /**
     * 新增当前金融机构在指定行业下的一条企业评估数据。
     *
     * @param request 新增请求
     */
    void create(IndustryCompanyDataSaveRequest request);

    /**
     * 新增当前金融机构在指定行业下的一条待评估企业数据。
     *
     * @param request 新增请求
     */
    void createPending(IndustryCompanyDataSaveRequest request);

    /**
     * 修改当前金融机构在指定行业下的一条企业评估数据。
     *
     * @param id 主键ID
     * @param request 修改请求
     */
    void update(Long id, IndustryCompanyDataSaveRequest request);

    /**
     * 修改当前金融机构在指定行业下的一条待评估企业数据。
     *
     * @param id 主键ID
     * @param request 修改请求
     */
    void updatePending(Long id, IndustryCompanyDataSaveRequest request);

    /**
     * 删除当前金融机构在指定行业下的一条企业评估数据。
     *
     * @param industryKey 行业键
     * @param institutionId 金融机构编码
     * @param id 主键ID
     */
    void deleteById(String industryKey, String institutionId, Long id);

    /**
     * 删除当前金融机构在指定行业下的一条待评估企业数据。
     *
     * @param industryKey 行业键
     * @param institutionId 金融机构编码
     * @param id 主键ID
     */
    void deletePendingById(String industryKey, String institutionId, Long id);

    /**
     * 调用模型服务预测待评估企业评分，并写回待评估数据表。
     *
     * @param id 待评估数据主键ID
     * @param institutionId 金融机构编码
     * @param industryKey 行业键
     */
    void scorePending(Long id, String institutionId, String industryKey, String modelMode);

    /**
     * 将待评估企业数据按授信结果归档到已评估历史表。
     *
     * @param id 待评估数据主键ID
     * @param institutionId 金融机构编码
     * @param industryKey 行业键
     */
    void creditPending(Long id, String institutionId, String industryKey);

    /**
     * 查询当前金融机构下某企业最近三年的转型贷款评分趋势。
     *
     * @param institutionId 金融机构编码
     * @param industryKey 行业键
     * @param companyCode 企业编码
     * @return 年份升序的评分趋势
     */
    List<TransitionScoreTrendVO> transitionScoreTrend(String institutionId, String industryKey, String companyCode);
}
