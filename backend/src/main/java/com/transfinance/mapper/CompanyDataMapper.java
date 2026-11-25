package com.transfinance.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 企业端跨行业数据查询 Mapper。
 *
 * <p>表名和查询字段由后端行业白名单拼接，前端参数不会直接控制动态 SQL。</p>
 */
public interface CompanyDataMapper {

    /**
     * 分页查询绑定企业的历史数据或待评估数据。
     */
    @Select("""
            <script>
            select d.*
             from (${unionSql}) d
             where d.companyName = #{companyName}
               and (
                    d.stockCode = #{stockCode}
                 or coalesce(nullif(trim(d.stockCode), ''), '-') = '-'
                 or coalesce(nullif(trim(#{stockCode}), ''), '-') = '-'
               )
            <if test="industryKey != null and industryKey != ''">
               and d.industryKey = #{industryKey}
            </if>
            <if test="dataYear != null">
               and d.dataYear = #{dataYear}
            </if>
             order by d.dataYear desc, d.institutionName, d.id desc
             limit #{offset}, #{size}
            </script>
            """)
    List<Map<String, Object>> selectCompanyDataPage(@Param("unionSql") String unionSql,
                                                    @Param("companyName") String companyName,
                                                    @Param("stockCode") String stockCode,
                                                    @Param("industryKey") String industryKey,
                                                    @Param("dataYear") Integer dataYear,
                                                    @Param("offset") Long offset,
                                                    @Param("size") Long size);

    /**
     * 统计绑定企业的历史数据或待评估数据。
     */
    @Select("""
            <script>
            select count(1)
             from (${unionSql}) d
             where d.companyName = #{companyName}
               and (
                    d.stockCode = #{stockCode}
                 or coalesce(nullif(trim(d.stockCode), ''), '-') = '-'
                 or coalesce(nullif(trim(#{stockCode}), ''), '-') = '-'
               )
            <if test="industryKey != null and industryKey != ''">
               and d.industryKey = #{industryKey}
            </if>
            <if test="dataYear != null">
               and d.dataYear = #{dataYear}
            </if>
            </script>
            """)
    Long countCompanyData(@Param("unionSql") String unionSql,
                          @Param("companyName") String companyName,
                          @Param("stockCode") String stockCode,
                          @Param("industryKey") String industryKey,
                          @Param("dataYear") Integer dataYear);

    /**
     * 查询绑定企业拥有数据的行业列表。
     */
    @Select("""
            <script>
            select distinct d.industryKey, d.industryName
             from (${unionSql}) d
             where d.companyName = #{companyName}
               and (
                    d.stockCode = #{stockCode}
                 or coalesce(nullif(trim(d.stockCode), ''), '-') = '-'
                 or coalesce(nullif(trim(#{stockCode}), ''), '-') = '-'
               )
             order by d.industryName
            </script>
            """)
    List<Map<String, Object>> selectCompanyIndustries(@Param("unionSql") String unionSql,
                                                      @Param("companyName") String companyName,
                                                      @Param("stockCode") String stockCode);

    /**
     * 查询绑定企业拥有数据的年份列表。
     */
    @Select("""
            <script>
            select distinct d.dataYear
             from (${unionSql}) d
             where d.companyName = #{companyName}
               and (
                    d.stockCode = #{stockCode}
                 or coalesce(nullif(trim(d.stockCode), ''), '-') = '-'
                 or coalesce(nullif(trim(#{stockCode}), ''), '-') = '-'
               )
            <if test="industryKey != null and industryKey != ''">
               and d.industryKey = #{industryKey}
            </if>
             order by d.dataYear desc
            </script>
            """)
    List<Integer> selectCompanyYears(@Param("unionSql") String unionSql,
                                     @Param("companyName") String companyName,
                                     @Param("stockCode") String stockCode,
                                     @Param("industryKey") String industryKey);

    /**
     * 查询企业绑定的某条行业数据详情。
     */
    @Select("""
            select ${selectColumns}
              from ${tableName} t
             where t.id = #{id}
               and t.company_name = #{companyName}
               and (
                    t.stock_code = #{stockCode}
                 or coalesce(nullif(trim(t.stock_code), ''), '-') = '-'
                 or coalesce(nullif(trim(#{stockCode}), ''), '-') = '-'
               )
            """)
    Map<String, Object> selectCompanyDataDetail(@Param("tableName") String tableName,
                                                @Param("selectColumns") String selectColumns,
                                                @Param("id") Long id,
                                                @Param("companyName") String companyName,
                                                @Param("stockCode") String stockCode);
}
