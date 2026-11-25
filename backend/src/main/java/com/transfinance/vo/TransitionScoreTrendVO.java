package com.transfinance.vo;

import java.math.BigDecimal;

/**
 * 企业近三年转型贷款评分趋势。
 */
public class TransitionScoreTrendVO {

    /** 数据年份。 */
    private Integer dataYear;

    /** 转型贷款评分。 */
    private BigDecimal transitionScore;

    public Integer getDataYear() {
        return dataYear;
    }

    public void setDataYear(Integer dataYear) {
        this.dataYear = dataYear;
    }

    public BigDecimal getTransitionScore() {
        return transitionScore;
    }

    public void setTransitionScore(BigDecimal transitionScore) {
        this.transitionScore = transitionScore;
    }
}
