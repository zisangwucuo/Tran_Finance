package com.transfinance.mapper;

import com.transfinance.vo.CompanyMissingReminderVO;
import com.transfinance.vo.FinancialDataChangeNoticeVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 企业缺失数据提醒与补全 Mapper。
 */
public interface CompanyMissingDataMapper {

    @Select("""
             select max(data_year)
               from ${tableName}
              where institution_code = #{institutionCode}
                 or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
            """)
    Integer selectMaxYear(@Param("tableName") String tableName,
                          @Param("institutionCode") String institutionCode,
                          @Param("institutionName") String institutionName);

    @Select("""
            select distinct company_name, stock_code, company_type, industry_name
              from ${tableName}
             where company_name is not null
               and company_name <> ''
             order by company_name
            """)
    List<Map<String, Object>> selectDistinctCompanies(@Param("tableName") String tableName);

    @Select("""
            select ${selectColumns}
              from ${tableName}
             where (
                       institution_code = #{institutionCode}
                       or (coalesce(institution_code, '') = '' and institution_name = #{institutionName})
                   )
               and data_year < #{maxYear}
            order by data_year, company_name, id
            """)
    List<Map<String, Object>> selectTrainingRows(@Param("tableName") String tableName,
                                                 @Param("selectColumns") String selectColumns,
                                                 @Param("institutionCode") String institutionCode,
                                                 @Param("institutionName") String institutionName,
                                                 @Param("maxYear") Integer maxYear);

    @Select("select ${selectColumns} from ${tableName} where id = #{id}")
    Map<String, Object> selectSourceRow(@Param("tableName") String tableName,
                                        @Param("selectColumns") String selectColumns,
                                        @Param("id") Long id);

    @Insert("""
            insert into company_data_missing_reminder
                (company_code, company_name, institution_code, institution_name, industry_key, industry_name,
                 source_table, source_row_id, data_year, missing_columns_json, status, created_at)
            values
                (#{companyCode}, #{companyName}, #{institutionCode}, #{institutionName}, #{industryKey}, #{industryName},
                 #{sourceTable}, #{sourceRowId}, #{dataYear}, cast(#{missingColumnsJson} as json), 'PENDING', now())
            on duplicate key update
                 company_name = values(company_name),
                 institution_name = values(institution_name),
                 industry_name = values(industry_name),
                 missing_columns_json = values(missing_columns_json),
                 status = 'PENDING',
                 created_at = now()
            """)
    int upsertReminder(@Param("companyCode") String companyCode,
                       @Param("companyName") String companyName,
                       @Param("institutionCode") String institutionCode,
                       @Param("institutionName") String institutionName,
                       @Param("industryKey") String industryKey,
                       @Param("industryName") String industryName,
                       @Param("sourceTable") String sourceTable,
                       @Param("sourceRowId") Long sourceRowId,
                       @Param("dataYear") Integer dataYear,
                       @Param("missingColumnsJson") String missingColumnsJson);

    @Select("""
            select id, company_code, company_name, institution_code, institution_name, industry_key, industry_name,
                   source_table, source_row_id, data_year, cast(missing_columns_json as char) as missing_columns_json,
                   cast(changed_columns_json as char) as changed_columns_json, status, created_at, submitted_at
              from company_data_missing_reminder
             where company_code = #{companyCode}
               and status = 'PENDING'
             order by created_at desc, id desc
             limit #{offset}, #{size}
            """)
    List<CompanyMissingReminderVO> selectCompanyReminderPage(@Param("companyCode") String companyCode,
                                                            @Param("offset") Long offset,
                                                            @Param("size") Long size);

    @Select("""
            select count(1)
              from company_data_missing_reminder
             where company_code = #{companyCode}
               and status = 'PENDING'
            """)
    Long countCompanyReminders(@Param("companyCode") String companyCode);

    @Select("""
            select id, company_code, company_name, institution_code, institution_name, industry_key, industry_name,
                   source_table, source_row_id, data_year, cast(missing_columns_json as char) as missing_columns_json,
                   cast(changed_columns_json as char) as changed_columns_json, status, created_at, submitted_at
              from company_data_missing_reminder
             where id = #{id}
               and company_code = #{companyCode}
            """)
    CompanyMissingReminderVO selectReminderForCompany(@Param("id") Long id,
                                                     @Param("companyCode") String companyCode);

    @Update("""
            update ${tableName}
               set ${setSql}
             where id = #{id}
            """)
    int updateSourceRow(@Param("tableName") String tableName,
                        @Param("setSql") String setSql,
                        @Param("id") Long id,
                        @Param("params") Map<String, Object> params);

    @Update("""
            update company_data_missing_reminder
               set changed_columns_json = cast(#{changedColumnsJson} as json)
             where id = #{id}
               and company_code = #{companyCode}
               and status = 'PENDING'
            """)
    int updateChangedColumns(@Param("id") Long id,
                             @Param("companyCode") String companyCode,
                             @Param("changedColumnsJson") String changedColumnsJson);

    @Update("""
            update company_data_missing_reminder
               set status = 'SUBMITTED',
                   submitted_at = now()
             where id = #{id}
               and company_code = #{companyCode}
               and status = 'PENDING'
            """)
    int submitReminder(@Param("id") Long id,
                       @Param("companyCode") String companyCode);

    @Insert("""
            insert into company_data_change_notice
                (institution_code, company_code, company_name, industry_key, industry_name, source_table,
                 source_row_id, data_year, changed_columns_json, created_at)
            values
                (#{institutionCode}, #{companyCode}, #{companyName}, #{industryKey}, #{industryName}, #{sourceTable},
                 #{sourceRowId}, #{dataYear}, cast(#{changedColumnsJson} as json), now())
            """)
    int insertChangeNotice(@Param("institutionCode") String institutionCode,
                           @Param("companyCode") String companyCode,
                           @Param("companyName") String companyName,
                           @Param("industryKey") String industryKey,
                           @Param("industryName") String industryName,
                           @Param("sourceTable") String sourceTable,
                           @Param("sourceRowId") Long sourceRowId,
                           @Param("dataYear") Integer dataYear,
                           @Param("changedColumnsJson") String changedColumnsJson);

    @Select("""
            <script>
            select id, institution_code, company_code, company_name, industry_key, industry_name, source_table,
                   source_row_id, data_year,
                   cast(missing_columns_json as char) as missing_columns_json,
                   cast(changed_columns_json as char) as changed_columns_json,
                   status, created_at, submitted_at, null as read_at
              from company_data_missing_reminder
             where institution_code = #{institutionCode}
             <if test="keyword != null and keyword != ''">
               and (
                    company_code like concat('%', #{keyword}, '%')
               )
             </if>
             <if test="industryKey != null and industryKey != ''">
               and industry_key = #{industryKey}
             </if>
             <if test="dataYear != null">
               and data_year = #{dataYear}
             </if>
             <if test="status != null and status != ''">
               and status = #{status}
             </if>
             order by case when status = 'PENDING' then 0 else 1 end, created_at desc, id desc
             limit #{offset}, #{size}
            </script>
            """)
    List<FinancialDataChangeNoticeVO> selectFinancialNoticePage(@Param("institutionCode") String institutionCode,
                                                                @Param("keyword") String keyword,
                                                                @Param("industryKey") String industryKey,
                                                                @Param("dataYear") Integer dataYear,
                                                                @Param("status") String status,
                                                                @Param("offset") Long offset,
                                                                @Param("size") Long size);

    @Select("""
            <script>
            select count(1)
              from company_data_missing_reminder
             where institution_code = #{institutionCode}
             <if test="keyword != null and keyword != ''">
               and (
                    company_code like concat('%', #{keyword}, '%')
               )
             </if>
             <if test="industryKey != null and industryKey != ''">
               and industry_key = #{industryKey}
             </if>
             <if test="dataYear != null">
               and data_year = #{dataYear}
             </if>
             <if test="status != null and status != ''">
               and status = #{status}
             </if>
            </script>
            """)
    Long countFinancialNotices(@Param("institutionCode") String institutionCode,
                               @Param("keyword") String keyword,
                               @Param("industryKey") String industryKey,
                               @Param("dataYear") Integer dataYear,
                               @Param("status") String status);

}
