-- 2026-08-27 联邦学习模型元数据表
set names utf8mb4;

create table if not exists `federated_model_metadata` (
  `id` bigint primary key auto_increment comment '联邦模型元数据ID',
  `industry_key` varchar(64) not null comment '行业模型编码',
  `industry_name` varchar(100) not null comment '行业名称',
  `target_name` varchar(64) not null comment '目标变量',
  `model_type` varchar(100) not null comment '模型类型',
  `model_path` varchar(500) not null comment '模型文件路径',
  `feature_columns_json` json null comment '特征字段列表',
  `feature_weights_json` json null comment '聚合特征重要性',
  `metrics_json` json null comment '聚合评估指标',
  `participant_summary_json` json null comment '参与机构摘要',
  `participant_count` int not null default 0 comment '参与机构数量',
  `sample_count` int not null default 0 comment '聚合样本数',
  `company_count` int not null default 0 comment '聚合企业数',
  `year_start` int null comment '训练集起始年份',
  `year_end` int null comment '训练集结束年份',
  `model_version` varchar(64) not null comment '模型版本',
  `status` varchar(30) not null comment '模型状态',
  `trained_by` varchar(64) null comment '发起训练的机构编码',
  `trained_at` datetime null comment '训练完成时间',
  `updated_at` datetime not null default current_timestamp on update current_timestamp comment '更新时间',
  unique key `uk_federated_model_scope` (`industry_key`, `target_name`),
  key `idx_federated_model_status` (`status`, `trained_at`)
) engine=InnoDB default charset=utf8mb4 comment='联邦行业模型元数据表';
