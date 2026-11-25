-- 2026-08-16 企业端账号、缺失数据提醒、补全通知表

set names utf8mb4;

create table if not exists `company` (
  `company_code` varchar(100) primary key comment '企业编码，由企业名称拼音首字母生成',
  `company_name` varchar(200) not null comment '企业名称',
  `company_type` varchar(30) not null comment '企业类型：上市企业/非上市企业',
  `stock_code` varchar(64) null comment '股票代码，非上市企业可为-',
  `industry_key` varchar(64) not null comment '所属行业键',
  `industry_name` varchar(100) not null comment '所属行业名称',
  `contact_name` varchar(100) null comment '联系人',
  `contact_phone` varchar(30) null comment '联系方式',
  `address` varchar(500) null comment '地址',
  `created_at` datetime not null default current_timestamp comment '创建时间',
  `updated_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
  key `idx_company_name` (`company_name`),
  key `idx_company_stock_code` (`stock_code`)
) engine=InnoDB default charset=utf8mb4 comment='企业基础信息表';

create table if not exists `company_user` (
  `id` bigint primary key auto_increment comment '企业用户ID',
  `company_code` varchar(100) not null comment '绑定企业编码',
  `username` varchar(64) not null comment '登录用户名',
  `password_hash` varchar(100) not null comment '登录密码字段',
  `created_at` datetime not null default current_timestamp comment '创建时间',
  unique key `uk_company_user_username` (`username`),
  key `idx_company_user_company` (`company_code`)
) engine=InnoDB default charset=utf8mb4 comment='企业登录用户表';

create table if not exists `company_data_missing_reminder` (
  `id` bigint primary key auto_increment comment '缺失提醒ID',
  `company_code` varchar(100) not null comment '企业编码',
  `company_name` varchar(200) not null comment '企业名称',
  `institution_code` varchar(64) not null comment '金融机构编码',
  `institution_name` varchar(200) null comment '金融机构名称',
  `industry_key` varchar(64) not null comment '行业键',
  `industry_name` varchar(100) not null comment '行业名称',
  `source_table` varchar(128) not null comment '缺失数据所在行业历史表',
  `source_row_id` bigint not null comment '缺失数据行ID',
  `data_year` int not null comment '数据年份',
  `missing_columns_json` json not null comment '缺失字段列表',
  `changed_columns_json` json null comment '企业补全字段及新旧值',
  `status` varchar(30) not null default 'PENDING' comment '状态：PENDING/SUBMITTED',
  `created_at` datetime not null default current_timestamp comment '创建时间',
  `submitted_at` datetime null comment '企业提交时间',
  unique key `uk_missing_source_company` (`source_table`, `source_row_id`, `company_code`),
  key `idx_missing_company_status` (`company_code`, `status`),
  key `idx_missing_institution` (`institution_code`, `status`)
) engine=InnoDB default charset=utf8mb4 comment='企业缺失数据补全提醒表';

create table if not exists `company_data_change_notice` (
  `id` bigint primary key auto_increment comment '补全通知ID',
  `institution_code` varchar(64) not null comment '金融机构编码',
  `company_code` varchar(100) not null comment '企业编码',
  `company_name` varchar(200) not null comment '企业名称',
  `industry_key` varchar(64) not null comment '行业键',
  `industry_name` varchar(100) not null comment '行业名称',
  `source_table` varchar(128) not null comment '被补全的数据表',
  `source_row_id` bigint not null comment '被补全的数据行ID',
  `data_year` int not null comment '数据年份',
  `changed_columns_json` json not null comment '补全字段及新旧值',
  `created_at` datetime not null default current_timestamp comment '创建时间',
  `read_at` datetime null comment '金融机构查看时间',
  key `idx_notice_institution` (`institution_code`, `created_at`),
  key `idx_notice_company` (`company_code`, `created_at`)
) engine=InnoDB default charset=utf8mb4 comment='企业补全结果通知表';
