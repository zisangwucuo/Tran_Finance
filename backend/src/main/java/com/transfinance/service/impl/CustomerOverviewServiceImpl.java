package com.transfinance.service.impl;

import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.CustomerOverviewMapper;
import com.transfinance.mapper.FinancialInstitutionMapper;
import com.transfinance.service.CustomerOverviewService;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.vo.CustomerOverviewVO;
import com.transfinance.vo.CustomerOverviewVO.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 将八个行业的历史和待评估记录转换成可解释的统计。
 * 统计由服务端完成，不受列表分页影响；只读事务使各次查询使用同一数据库快照。
 */
@Service
public class CustomerOverviewServiceImpl implements CustomerOverviewService {
    private final CustomerOverviewMapper mapper;
    private final FinancialInstitutionMapper institutionMapper;

    public CustomerOverviewServiceImpl(CustomerOverviewMapper mapper, FinancialInstitutionMapper institutionMapper) {
        this.mapper = mapper;
        this.institutionMapper = institutionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOverviewVO overview(String institutionCode, Integer dataYear) {
        if (institutionCode == null || institutionCode.isBlank()) throw new BusinessException(401, "请重新登录金融机构账号");
        if (dataYear != null && (dataYear < 2000 || dataYear > 2100)) throw new BusinessException("年份应在2000至2100之间");
        var institution = institutionMapper.selectById(institutionCode);
        if (institution == null) throw new BusinessException("当前金融机构不存在，请重新登录");

        var totals = new Accumulator();
        var byYear = new TreeMap<Integer, Accumulator>();
        var years = new TreeSet<Integer>(Comparator.reverseOrder());
        var industries = new ArrayList<Industry>();
        // 固定排序保证零数据行业也稳定显示，表名与行业标识均不接受客户端输入。
        var catalog = IndustryDataCatalog.industries().values().stream()
                .sorted(Comparator.comparing(IndustryDataCatalog.IndustryMeta::key)).toList();
        for (var industry : catalog) {
            var group = new Accumulator();
            for (boolean pending : List.of(false, true)) {
                String table = pending ? industry.pendingTableName() : industry.tableName();
                for (var row : mapper.selectRecords(table, institutionCode, institution.getInstitutionName())) {
                    Integer year = row.get("dataYear") instanceof Number number ? number.intValue() : null;
                    if (year != null) years.add(year);
                    if (dataYear != null && !dataYear.equals(year)) continue;
                    totals.add(row, pending);
                    group.add(row, pending);
                    if (year != null) byYear.computeIfAbsent(year, ignored -> new Accumulator()).add(row, pending);
                }
            }
            industries.add(new Industry(industry.key(), industry.name(), group.codes.size(), group.history,
                    group.pending, group.pending - group.pendingScored, group.average()));
        }
        industries.sort(Comparator.comparingLong(Industry::customers).reversed().thenComparing(Industry::key));
        long pendingNotices = 0, submittedNotices = 0;
        for (var row : mapper.selectNoticeCounts(institutionCode, dataYear)) {
            long count = ((Number) row.get("total")).longValue();
            if ("PENDING".equals(row.get("status"))) pendingNotices += count;
            if ("SUBMITTED".equals(row.get("status"))) submittedNotices += count;
        }
        var summary = new Totals(totals.codes.size(), (int) industries.stream()
                .filter(item -> item.historyRecords() + item.pendingRecords() > 0).count(),
                totals.records(), totals.history, totals.pending, totals.pending - totals.pendingScored,
                totals.pendingScored, totals.scored, totals.records() - totals.scored, totals.unresolved, totals.average());
        var yearly = byYear.entrySet().stream().map(entry -> new Year(entry.getKey(), entry.getValue().codes.size(),
                entry.getValue().history, entry.getValue().pending)).toList();
        var bands = List.of(new ScoreBand("低于40分", totals.bands[0]), new ScoreBand("40至不足60分", totals.bands[1]),
                new ScoreBand("60至不足80分", totals.bands[2]), new ScoreBand("80至100分", totals.bands[3]));
        return new CustomerOverviewVO(institution.getInstitutionName(), dataYear, List.copyOf(years),
                LocalDateTime.now(), summary, industries, yearly, bands, new Notices(pendingNotices, submittedNotices));
    }

    /** 内部累加器保留客户集合与记录数两个口径；缺失、非数值、越界评分均不进入均值。 */
    private static final class Accumulator {
        private final Set<String> codes = new HashSet<>();
        private long history, pending, pendingScored, scored, unresolved;
        private BigDecimal sum = BigDecimal.ZERO;
        private final long[] bands = new long[4];

        private void add(Map<String, Object> row, boolean isPending) {
            String code = Objects.toString(row.get("companyCode"), "").trim();
            if (code.isEmpty() || code.equals("-")) unresolved++;
            else codes.add(code);
            if (isPending) pending++;
            else history++;
            BigDecimal score;
            try { score = new BigDecimal(Objects.toString(row.get("transitionScore"), "")); }
            catch (NumberFormatException exception) { return; }
            if (score.signum() < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) return;
            scored++;
            if (isPending) pendingScored++;
            sum = sum.add(score);
            bands[score.compareTo(BigDecimal.valueOf(40)) < 0 ? 0 : score.compareTo(BigDecimal.valueOf(60)) < 0 ? 1
                    : score.compareTo(BigDecimal.valueOf(80)) < 0 ? 2 : 3]++;
        }

        private long records() { return history + pending; }
        private BigDecimal average() { return scored == 0 ? null : sum.divide(BigDecimal.valueOf(scored), 1, RoundingMode.HALF_UP); }
    }
}
