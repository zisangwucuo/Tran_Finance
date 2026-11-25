package com.transfinance.mapper;

import com.transfinance.support.CompanyIdentitySql;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 只读总览查询。表名仅来自 IndustryDataCatalog 白名单，机构与年份使用绑定参数。
 * 仅取统计所需字段，不加载整张指标宽表，也不向浏览器暴露企业名称。
 */
public interface CustomerOverviewMapper {
    /** 旧数据仅在机构名称唯一时才允许按名称归属，避免同名银行交叉计数。 */
    @Select("select " + CompanyIdentitySql.CODE + """
             as companyCode, t.data_year as dataYear, t.transition_score as transitionScore
             from ${tableName} t
             where t.institution_code = #{institutionCode}
                or (coalesce(t.institution_code, '') = '' and t.institution_name = #{institutionName}
                    and (select count(*) from financial_institution fi
                         where fi.institution_name = #{institutionName}) = 1)
            """)
    List<Map<String, Object>> selectRecords(@Param("tableName") String tableName,
                                           @Param("institutionCode") String institutionCode,
                                           @Param("institutionName") String institutionName);

    /** 补全任务以本行编码和所选年份统计；任务条数不等同于企业数。 */
    @Select("""
            <script>
            select status, count(*) as total
            from company_data_missing_reminder
            where institution_code = #{institutionCode}
            <if test="dataYear != null">and data_year = #{dataYear}</if>
            group by status
            </script>
            """)
    List<Map<String, Object>> selectNoticeCounts(@Param("institutionCode") String institutionCode,
                                               @Param("dataYear") Integer dataYear);
}
