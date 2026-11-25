package com.transfinance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.transfinance.entity.Company;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 企业基础信息 Mapper。
 */
public interface CompanyMapper extends BaseMapper<Company> {

    /**
     * 更新指定历史数据表中的企业名称和企业类型，确保企业资料修改后评估数据同步展示。
     */
    @Update("""
            update ${tableName}
               set company_name = #{companyName},
                   company_type = #{companyType}
             where company_name = #{oldCompanyName}
               and (
                   stock_code = #{stockCode}
                   or (stock_code is null and #{stockCode} is null)
                   or (#{stockCode} = '-' and stock_code is null)
                   or (#{stockCode} is null and stock_code = '-')
                   or (#{stockCode} = '' and stock_code = '-')
                   or (#{stockCode} = '-' and stock_code = '')
               )
            """)
    int syncCompanyProfileToDataTable(@Param("tableName") String tableName,
                                      @Param("oldCompanyName") String oldCompanyName,
                                      @Param("stockCode") String stockCode,
                                      @Param("companyName") String companyName,
                                      @Param("companyType") String companyType);
}
