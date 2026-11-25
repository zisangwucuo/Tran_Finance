-- 将八个行业历史数据表中的旧目标变量列重命名。
-- 仅改列名和索引名，不改动任何记录的 0/1/NULL 值。
-- 请在停止后端及模型服务后，对已有 trans_finance 数据库执行一次。

ALTER TABLE `nonferrous_metal_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `power_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `steel_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `aviation_airport_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `basic_chemical_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `building_material_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `coal_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

ALTER TABLE `petrochemical_company_data`
  RENAME COLUMN `transition_loan` TO `verified_transition_performance`,
  RENAME INDEX `idx_transition_loan` TO `idx_verified_transition_performance`;

-- 已训练模型的目标名也需同步，否则应用无法识别已有模型记录。
UPDATE `industry_model_metadata`
   SET `target_name` = 'verified_transition_performance'
 WHERE `target_name` = 'transition_loan';

UPDATE `federated_model_metadata`
   SET `target_name` = 'verified_transition_performance'
 WHERE `target_name` = 'transition_loan';
