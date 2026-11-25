package com.transfinance.vo;

import com.transfinance.common.PageResult;

import java.util.Map;
import java.util.List;

/**
 * 分行业企业评估数据分页响应。
 */
public class IndustryCompanyDataPageResponse {

    /** 当前行业键。 */
    private String industryKey;

    /** 当前行业名称。 */
    private String industryName;

    /** 当前行业表格列。 */
    private List<ColumnMetaVO> columns;

    /** 当前行业分页数据。 */
    private PageResult<Map<String, Object>> page;

    public String getIndustryKey() {
        return industryKey;
    }

    public void setIndustryKey(String industryKey) {
        this.industryKey = industryKey;
    }

    public String getIndustryName() {
        return industryName;
    }

    public void setIndustryName(String industryName) {
        this.industryName = industryName;
    }

    public List<ColumnMetaVO> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMetaVO> columns) {
        this.columns = columns;
    }

    public PageResult<Map<String, Object>> getPage() {
        return page;
    }

    public void setPage(PageResult<Map<String, Object>> page) {
        this.page = page;
    }
}
