package com.transfinance.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;
import com.transfinance.support.CompanyIdentitySql;

/** 材料任务、采用审计及企业数据操作。动态表名/列名仅来自后端行业目录。 */
@Mapper
public interface MaterialExtractionMapper {
    @Update("""
        CREATE TABLE IF NOT EXISTS company_material_task (
          id CHAR(36) PRIMARY KEY, company_code VARCHAR(100) NOT NULL,
          file_name VARCHAR(255) NOT NULL, industry_key VARCHAR(40) NOT NULL, data_year INT NOT NULL,
          status VARCHAR(24) NOT NULL, progress INT NOT NULL DEFAULT 0,
          message VARCHAR(500) NOT NULL DEFAULT '', result_json LONGTEXT NULL,
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          INDEX idx_material_owner (company_code, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)
    void createTasks();
    @Update("""
        CREATE TABLE IF NOT EXISTS company_material_adoption (
          id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id CHAR(36) NOT NULL,
          company_code VARCHAR(100) NOT NULL, source_table VARCHAR(100) NOT NULL,
          source_row_id BIGINT NOT NULL, changes_json LONGTEXT NOT NULL,
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          UNIQUE KEY uk_material_adoption (task_id, source_table, source_row_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)
    void createAudit();
    /** 进程重启不把未完成任务永久留在处理中；原文件和成功结果仍保留。 */
    @Update("UPDATE company_material_task SET status='FAILED', message='服务重启中断了任务，请重新上传提取' WHERE status IN ('QUEUED','PARSING','EXTRACTING')")
    void recover();
    @Insert("""
        INSERT INTO company_material_task (id,company_code,file_name,industry_key,data_year,status)
        VALUES (#{id},#{company},#{name},#{industry},#{year},'QUEUED')
        """)
    void insert(@Param("id") String id, @Param("company") String company, @Param("name") String name,
                @Param("industry") String industry, @Param("year") int year);
    String COLUMNS = "id, company_code AS companyCode, file_name AS fileName, industry_key AS industryKey, data_year AS dataYear, status, progress, message, result_json AS resultJson, created_at AS createdAt";
    @Select("SELECT " + COLUMNS + " FROM company_material_task WHERE company_code=#{company} ORDER BY created_at DESC,id DESC LIMIT 30")
    List<Map<String,Object>> list(@Param("company") String company);
    @Select("SELECT " + COLUMNS + " FROM company_material_task WHERE id=#{id} AND company_code=#{company}")
    Map<String,Object> get(@Param("company") String company, @Param("id") String id);
    @Update("UPDATE company_material_task SET status=#{status},progress=#{progress},message=#{message},result_json=#{result} WHERE id=#{id}")
    void update(@Param("id") String id, @Param("status") String status, @Param("progress") int progress,
                @Param("message") String message, @Param("result") String result);
    @Select("SELECT t.* FROM ${table} t WHERE " + CompanyIdentitySql.CODE + " = #{company} AND t.data_year=#{year} ORDER BY t.institution_code,t.id")
    List<Map<String,Object>> targets(@Param("table") String table, @Param("company") String company, @Param("year") int year);
    @Select("SELECT t.* FROM ${table} t WHERE t.id=#{id} AND " + CompanyIdentitySql.CODE + " = #{company} AND t.data_year=#{year} FOR UPDATE")
    Map<String,Object> lockTarget(@Param("table") String table, @Param("id") Long id,
                                 @Param("company") String company, @Param("year") int year);
    @Update("UPDATE ${table} SET ${assignments}, transition_score=NULL WHERE id=#{id}")
    int apply(@Param("table") String table, @Param("id") Long id, @Param("assignments") String assignments,
              @Param("values") Map<String,Object> values);
    @Insert("""
        INSERT INTO company_material_adoption (task_id,company_code,source_table,source_row_id,changes_json)
        VALUES (#{task},#{company},#{table},#{row},#{changes})
        """)
    void audit(@Param("task") String task, @Param("company") String company,
               @Param("table") String table, @Param("row") Long row, @Param("changes") String changes);
    @Select("SELECT source_row_id FROM company_material_adoption WHERE task_id=#{task} AND company_code=#{company}")
    List<Long> adoptedRows(@Param("task") String task, @Param("company") String company);
}
