package com.transfinance.vo;

import com.transfinance.common.PageResult;

/**
 * 企业端评估数据分页响应。
 */
public class CompanyAssessmentPageResponse {

    /** 数据集类型：history 或 pending。 */
    private String datasetType;

    /** 企业编码。 */
    private String companyCode;

    /** 企业名称。 */
    private String companyName;

    /** 分页记录。 */
    private PageResult<CompanyAssessmentRecordVO> page;

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public PageResult<CompanyAssessmentRecordVO> getPage() {
        return page;
    }

    public void setPage(PageResult<CompanyAssessmentRecordVO> page) {
        this.page = page;
    }
}
