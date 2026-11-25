package com.transfinance.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 本机构客户总览。仅返回聚合结果，不返回企业名称或其他银行的数据。
 * customers 按企业编码去重；其余 record 类计数按业务记录计算，可含同一企业多个年份。
 */
public record CustomerOverviewVO(String institutionName, Integer dataYear, List<Integer> years,
                                 LocalDateTime generatedAt, Totals totals, List<Industry> industries,
                                 List<Year> yearly, List<ScoreBand> scoreBands, Notices notices) {
    /** 未能唯一关联企业编码的记录计入记录总数，不计入客户数。无有效评分时平均分为 null。 */
    public record Totals(long customers, int coveredIndustries, long records, long historyRecords,
                         long pendingRecords, long pendingUnscored, long pendingScored,
                         long scoredRecords, long unscoredRecords, long unresolvedRecords,
                         BigDecimal averageScore) { }

    public record Industry(String key, String name, long customers, long historyRecords,
                           long pendingRecords, long pendingUnscored, BigDecimal averageScore) { }

    /** 年度客户数分别去重，年度之和不等于跨年度客户总数。 */
    public record Year(Integer year, long customers, long historyRecords, long pendingRecords) { }

    /** 区间采用左闭右开，最后一档包含100分；不附加未经验证的风险等级。 */
    public record ScoreBand(String label, long count) { }

    public record Notices(long pending, long submitted) { }
}
