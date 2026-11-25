package com.transfinance.vo;

import java.util.List;

/**
 * 企业评估数据筛选项响应。
 */
public class IndustryOptionsResponse {

    /** 当前金融机构可查看的行业。 */
    private List<IndustryOptionVO> industries;

    /** 当前行业下可查看的年份。 */
    private List<Integer> years;

    public List<IndustryOptionVO> getIndustries() {
        return industries;
    }

    public void setIndustries(List<IndustryOptionVO> industries) {
        this.industries = industries;
    }

    public List<Integer> getYears() {
        return years;
    }

    public void setYears(List<Integer> years) {
        this.years = years;
    }
}
