-- 2026-08-15 模型训练任务与模型元数据表

set names utf8mb4;

create table if not exists `model_training_job` (
  `id` bigint primary key auto_increment comment '训练任务ID',
  `institution_code` varchar(64) not null comment '金融机构编码',
  `job_type` varchar(30) not null comment '任务类型：train_all/update',
  `status` varchar(30) not null comment '任务状态：PENDING/RUNNING/SUCCESS/FAILED',
  `progress` int not null default 0 comment '训练进度，0-100',
  `message` varchar(500) null comment '任务说明',
  `total_models` int not null default 0 comment '计划训练模型数',
  `trained_models` int not null default 0 comment '已训练模型数',
  `started_at` datetime null comment '开始时间',
  `finished_at` datetime null comment '结束时间',
  `created_at` datetime not null default current_timestamp comment '创建时间',
  key `idx_training_job_institution` (`institution_code`, `created_at`)
) engine=InnoDB default charset=utf8mb4 comment='模型训练任务表';

create table if not exists `industry_model_metadata` (
  `id` bigint primary key auto_increment comment '模型元数据ID',
  `institution_code` varchar(64) not null comment '金融机构编码',
  `industry_key` varchar(64) not null comment '行业模型编码',
  `industry_name` varchar(100) not null comment '模型范围名称',
  `target_name` varchar(64) not null comment '目标变量：verified_transition_performance',
  `model_type` varchar(100) not null comment '模型类型',
  `model_path` varchar(500) not null comment '模型文件路径',
  `feature_columns_json` json null comment '特征字段列表',
  `feature_weights_json` json null comment '特征权重列表',
  `metrics_json` json null comment '模型训练评估指标',
  `sample_count` int not null default 0 comment '训练样本数',
  `company_count` int not null default 0 comment '训练企业数',
  `year_start` int null comment '训练集起始年份',
  `year_end` int null comment '训练集结束年份',
  `model_version` varchar(64) not null comment '模型版本',
  `status` varchar(30) not null comment '模型状态',
  `trained_at` datetime null comment '训练完成时间',
  `updated_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
  unique key `uk_model_scope` (`institution_code`, `industry_key`, `target_name`),
  key `idx_model_institution` (`institution_code`, `industry_key`)
) engine=InnoDB default charset=utf8mb4 comment='行业模型元数据表';
