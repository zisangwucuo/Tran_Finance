-- 企业PDF材料提取任务与采用审计；不重建或清空原有业务表。
CREATE TABLE IF NOT EXISTS company_material_task (
  id CHAR(36) PRIMARY KEY, company_code VARCHAR(100) NOT NULL,
  file_name VARCHAR(255) NOT NULL, industry_key VARCHAR(40) NOT NULL, data_year INT NOT NULL,
  status VARCHAR(24) NOT NULL, progress INT NOT NULL DEFAULT 0,
  message VARCHAR(500) NOT NULL DEFAULT '', result_json LONGTEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_material_owner (company_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS company_material_adoption (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id CHAR(36) NOT NULL,
  company_code VARCHAR(100) NOT NULL, source_table VARCHAR(100) NOT NULL,
  source_row_id BIGINT NOT NULL, changes_json LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_adoption (task_id, source_table, source_row_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
