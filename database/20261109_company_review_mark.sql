-- 本行企业关注标记：机构与企业联合唯一，刷新、重新登录后仍保留。
-- 已有数据库也可由后端首次使用标记功能时自动创建此表，无须重建业务数据。
CREATE TABLE IF NOT EXISTS company_review_mark (
    institution_code VARCHAR(100) NOT NULL,
    company_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (institution_code, company_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
