package com.transfinance.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.transfinance.entity.Company;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.CompanyMapper;
import java.util.List;

/** 企业初始化和缺失提醒共用的身份匹配规则，避免缺少股票代码时重复建档。 */
public final class CompanyIdentityResolver {
    private CompanyIdentityResolver() { }

    public static Company find(CompanyMapper mapper, String name, String industry, String stock) {
        return resolve(mapper.selectList(new LambdaQueryWrapper<Company>()
                .eq(Company::getCompanyName, name).eq(Company::getIndustryKey, industry)), stock);
    }

    /**
     * 候选必须已限定为同名同行业。股票代码缺失时只接受唯一候选；
     * 有股票代码时优先精确匹配，也允许复用唯一的缺代码主档。
     * 不把“存在冲突”和“完全不存在”混为一谈，前者拒绝自动建档。
     */
    public static Company resolve(List<Company> candidates, String stock) {
        if (candidates.isEmpty()) return null;
        String normalized = normalize(stock);
        if (!normalized.isEmpty()) {
            var exact = candidates.stream().filter(c -> normalized.equals(normalize(c.getStockCode()))).toList();
            if (exact.size() == 1) return exact.get(0);
            if (exact.isEmpty() && candidates.size() == 1 && normalize(candidates.get(0).getStockCode()).isEmpty())
                return candidates.get(0);
        } else if (candidates.size() == 1) {
            return candidates.get(0);
        }
        throw new BusinessException("企业身份存在歧义或股票代码冲突，请先核实企业主档，系统未自动创建新企业");
    }

    public static String normalize(String stock) {
        return stock == null || stock.isBlank() || "-".equals(stock.trim()) ? "" : stock.trim();
    }
}
