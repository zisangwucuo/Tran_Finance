package com.transfinance.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

/** 本行人工关注标记独立存储，不修改企业原始指标，不向其他机构共享。 */
@Mapper
public interface CompanyReviewMarkMapper {
    @Update("""
        CREATE TABLE IF NOT EXISTS company_review_mark (
          institution_code VARCHAR(100) NOT NULL,
          company_code VARCHAR(100) NOT NULL,
          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY (institution_code, company_code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """)
    void ensureTable();

    @Select("SELECT company_code FROM company_review_mark WHERE institution_code = #{bank}")
    List<String> findCodes(@Param("bank") String bank);

    /** 唯一键使重复点击幂等，不覆盖首次标记时间。 */
    @Insert("INSERT INTO company_review_mark (institution_code, company_code) VALUES (#{bank}, #{code}) ON DUPLICATE KEY UPDATE company_code = VALUES(company_code)")
    void mark(@Param("bank") String bank, @Param("code") String code);
}
