package com.transfinance.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 八个行业企业评估数据通用查询 Mapper。
 */
public interface IndustryCompanyDataMapper {

    /**
     * 分页查询指定行业表中的企业评估数据。
     */
    @Select("""
            <script>
            select ${selectColumns}
            from ${tableName} t
            where (
                t.institution_code = #{institutionCode}
                or (coalesce(t.institution_code, '') = '' and t.institution_name = #{institutionName})
            )
            <if test="companyName != null and companyName != ''">
              and t.company_name like concat('%', #{companyName}, '%')
            </if>
            <if test="companyCode != null and companyCode != ''">
              and
            """ + com.transfinance.support.CompanyIdentitySql.CODE + """
              like concat('%', #{companyCode}, '%')
            </if>
            <if test="stockCode != null and stockCode != ''">
              and t.stock_code like concat('%', #{stockCode}, '%')
            </if>
            <if test="dataYear != null">
              and t.data_year = #{dataYear}
            </if>
            <if test="verifiedTransitionPerformance != null">
              and t.verified_transition_performance = #{verifiedTransitionPerformance}
            </if>
            order by t.data_year desc, t.id desc
            limit #{offset}, #{size}
            </script>
            """)
    List<Map<String, Object>> selectPageRows(@Param("tableName") String tableName,
                                             @Param("selectColumns") String selectColumns,
                                             @Param("institutionCode") String institutionCode,
                                             @Param("institutionName") String institutionName,
                                             @Param("companyName") String companyName,
                                             @Param("companyCode") String companyCode,
                                             @Param("stockCode") String stockCode,
                                             @Param("dataYear") Integer dataYear,
                                             @Param("verifiedTransitionPerformance") Integer verifiedTransitionPerformance,
                                             @Param("offset") Long offset,
                                             @Param("size") Long size);

    /**
     * 按ID查询当前金融机构拥有的一条指定行业企业数据。
     */
    @Select("""
            select ${selectColumns}
            from ${tableName} t
            where t.id = #{id}
              and (
                  t.institution_code = #{institutionCode}
                  or (coalesce(t.institution_code, '') = '' and t.institution_name = #{institutionName})
              )
            """)
    Map<String, Object> selectRowById(@Param("tableName") String tableName,
                                      @Param("selectColumns") String selectColumns,
                                      @Param("id") Long id,
                                      @Param("institutionCode") String institutionCode,
                                      @Param("institutionName") String institutionName);

    /**
     * 统计指定行业表中符合筛选条件的数据量。
     */
    @Select("""
            <script>
            select count(1)
            from ${tableName} t
            where (
                t.institution_code = #{institutionCode}
                or (coalesce(t.institution_code, '') = '' and t.institution_name = #{institutionName})
            )
            <if test="companyName != null and companyName != ''">
              and t.company_name like concat('%', #{companyName}, '%')
            </if>
            <if test="companyCode != null and companyCode != ''">
              and
            """ + com.transfinance.support.CompanyIdentitySql.CODE + """
              like concat('%', #{companyCode}, '%')
            </if>
            <if test="stockCode != null and stockCode != ''">
              and t.stock_code like concat('%', #{stockCode}, '%')
            </if>
            <if test="dataYear != null">
              and t.data_year = #{dataYear}
            </if>
            <if test="verifiedTransitionPerformance != null">
              and t.verified_transition_performance = #{verifiedTransitionPerformance}
            </if>
            </script>
            """)
    Long countRows(@Param("tableName") String tableName,
                   @Param("institutionCode") String institutionCode,
                   @Param("institutionName") String institutionName,
                   @Param("companyName") String companyName,
                   @Param("companyCode") String companyCode,
                   @Param("stockCode") String stockCode,
                   @Param("dataYear") Integer dataYear,
                   @Param("verifiedTransitionPerformance") Integer verifiedTransitionPerformance);

    /**
     * 查询指定企业在当前金融机构和行业中的最近三年转型贷款评分。
     */
    @Select("""
            select dataYear, transitionScore
            from (
                select t.data_year as dataYear, t.transition_score as transitionScore
                  from ${tableName} t
                 where (
                           t.institution_code = #{institutionCode}
                           or (coalesce(t.institution_code, '') = '' and t.institution_name = #{institutionName})
                       )
                   and t.transition_score is not null
                   and
            """ + com.transfinance.support.CompanyIdentitySql.CODE + """
                   = #{companyCode}
                order by t.data_year desc
                limit 3
            ) latest
            order by dataYear
            """)
    List<Map<String, Object>> selectTransitionScoreTrend(@Param("tableName") String tableName,
                                                         @Param("institutionCode") String institutionCode,
                                                         @Param("institutionName") String institutionName,
                                                         @Param("companyCode") String companyCode);

    /**
     * 查询当前金融机构在指定行业表中拥有的年份。
     */
    @Select("""
            select distinct data_year
            from ${tableName}
            where (
                institution_code = #{institutionCode}
                or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
            )
            order by data_year desc
            """)
    List<Integer> selectYears(@Param("tableName") String tableName,
                              @Param("institutionCode") String institutionCode,
                              @Param("institutionName") String institutionName);

    /**
     * 判断当前金融机构在指定行业表中是否有数据。
     */
    @Select("""
            select count(1)
            from ${tableName}
            where (
                institution_code = #{institutionCode}
                or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
            )
            """)
    Long countByInstitution(@Param("tableName") String tableName,
                            @Param("institutionCode") String institutionCode,
                            @Param("institutionName") String institutionName);

    /**
     * 新增指定行业表中的企业评估数据。
     */
    @Insert("""
            insert into ${tableName} (${insertColumns})
            values (${valuePlaceholders})
            """)
    int insertRow(@Param("tableName") String tableName,
                  @Param("insertColumns") String insertColumns,
                  @Param("valuePlaceholders") String valuePlaceholders,
                  @Param("params") Map<String, Object> params);

    /**
     * 修改指定行业表中当前金融机构拥有的一条企业评估数据。
     */
    @Update("""
            update ${tableName}
            set ${setSql}
            where id = #{id}
              and (
                  institution_code = #{institutionCode}
                  or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
              )
            """)
    int updateRow(@Param("tableName") String tableName,
                  @Param("setSql") String setSql,
                  @Param("id") Long id,
                  @Param("institutionCode") String institutionCode,
                  @Param("institutionName") String institutionName,
                  @Param("params") Map<String, Object> params);

    /**
     * 更新待评估企业数据的模型评分。
     */
    @Update("""
            update ${tableName}
               set transition_score = #{transitionScore}
             where id = #{id}
               and (
                   institution_code = #{institutionCode}
                   or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
               )
            """)
    int updateTransitionScore(@Param("tableName") String tableName,
                              @Param("id") Long id,
                              @Param("institutionCode") String institutionCode,
                              @Param("institutionName") String institutionName,
                              @Param("transitionScore") java.math.BigDecimal transitionScore);

    /**
     * 删除指定行业表中当前金融机构拥有的一条企业评估数据。
     */
    @Delete("""
            delete from ${tableName}
            where id = #{id}
              and (
                  institution_code = #{institutionCode}
                  or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
              )
            """)
    int deleteRow(@Param("tableName") String tableName,
                  @Param("id") Long id,
                  @Param("institutionCode") String institutionCode,
                  @Param("institutionName") String institutionName);
}
