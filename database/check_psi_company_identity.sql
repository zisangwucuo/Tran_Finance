-- PSI企业身份只读核查：不合并企业，不修改数据，不读取密码或联系方式。
-- 在当前业务数据库执行；示例：中煤能源、淮北矿业。

-- 1. 对照同名同行业的企业编码及股票代码。
SELECT company_code, company_name, company_type, stock_code, industry_key
FROM company
WHERE industry_key = 'coal'
  AND company_name IN ('中煤能源', '淮北矿业')
ORDER BY company_name, company_code;

-- 2. 对照历史数据中的股票代码、银行和年份。
SELECT company_name, institution_code, stock_code, data_year, id
FROM coal_company_data
WHERE company_name IN ('中煤能源', '淮北矿业')
ORDER BY company_name, institution_code, data_year, id;

-- 3. 对照待评估数据；股票代码缺失会使同名的两个企业主档同时匹配。
SELECT company_name, institution_code, stock_code, data_year, id
FROM coal_pending_company_data
WHERE company_name IN ('中煤能源', '淮北矿业')
ORDER BY company_name, institution_code, data_year, id;

-- 4. 确认各企业编码已有账号，避免未经核对就删除企业导致账号失去关联。
SELECT c.company_code, c.company_name, COUNT(u.id) AS account_count
FROM company c
LEFT JOIN company_user u ON u.company_code = c.company_code
WHERE c.industry_key = 'coal'
  AND c.company_name IN ('中煤能源', '淮北矿业')
GROUP BY c.company_code, c.company_name
ORDER BY c.company_name, c.company_code;
