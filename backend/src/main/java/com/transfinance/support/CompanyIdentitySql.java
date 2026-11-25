package com.transfinance.support;

/**
 * 历史、待评估列表及企业编码过滤共用的身份查询，外层业务表别名固定为t。
 * 股票代码规范化后优先唯一精确匹配；任一侧缺失时只接受同名同行业唯一主档。
 * 聚合加HAVING保证歧义时返回NULL，不使用LIMIT 1任意挑选，也不跨行业匹配。
 * 这里只生成固定SQL，不接收用户输入，所有筛选参数仍由MyBatis绑定。
 */
public final class CompanyIdentitySql {
    private CompanyIdentitySql() { }

    public static final String CODE = """
        coalesce(
          (select min(ci.company_code) from company ci
           where ci.company_name = t.company_name and ci.industry_name = t.industry_name
             and nullif(nullif(trim(t.stock_code), ''), '-') is not null
             and trim(ci.stock_code) = trim(t.stock_code)
           having count(*) = 1),
          case when nullif(nullif(trim(t.stock_code), ''), '-') is null then
            (select min(ci.company_code) from company ci
             where ci.company_name = t.company_name and ci.industry_name = t.industry_name
             having count(*) = 1)
          else
            (select case when count(*) = 1
                         and max(nullif(nullif(trim(ci.stock_code), ''), '-')) is null
                         then min(ci.company_code) end
             from company ci
             where ci.company_name = t.company_name and ci.industry_name = t.industry_name)
          end
        )
        """;
}
