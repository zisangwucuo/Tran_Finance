package com.transfinance.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

/** PSI页面仅读取机构公开名称，不再查询任何跨机构企业原始记录。 */
@Repository
public class CommonCustomerMapper {
    private final JdbcTemplate jdbc;
    public CommonCustomerMapper(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> institutions() {
        return jdbc.queryForList("select institution_code, institution_name from financial_institution"
                + " where institution_code in ('bank_a','bank_b','bank_c') order by institution_code");
    }
}
