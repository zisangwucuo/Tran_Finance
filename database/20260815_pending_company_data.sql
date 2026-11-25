-- 2026-08-15 八行业待评估企业数据初始化脚本（历史版本）
-- 2026-08-26 起请改为执行 database/20260826_evaluation_data_refresh.sql，
-- 该脚本会删除旧待评估表并导入新的 2023-2025 数据。
-- 说明：
-- 1. 每张待评估表从对应已评估历史表复制结构后，删除两个目标变量列。
-- 2. 初始化数据取每个金融机构在该行业历史表中的最新年份数据。
-- 3. transition_score 与 green_loan_score 暂置为空，等待后续评估流程写入或计算。

set names utf8mb4;

drop procedure if exists init_pending_company_data_table;

delimiter //

create procedure init_pending_company_data_table(
    in source_table_name varchar(128),
    in pending_table_name varchar(128)
)
begin
    set @drop_sql = concat('drop table if exists `', pending_table_name, '`');
    prepare drop_stmt from @drop_sql;
    execute drop_stmt;
    deallocate prepare drop_stmt;

    set @create_sql = concat('create table `', pending_table_name, '` like `', source_table_name, '`');
    prepare create_stmt from @create_sql;
    execute create_stmt;
    deallocate prepare create_stmt;

    set @alter_sql = concat(
        'alter table `', pending_table_name, '` ',
        'drop column `green_loan`, ',
        'drop column `verified_transition_performance`, ',
        'auto_increment = 1'
    );
    prepare alter_stmt from @alter_sql;
    execute alter_stmt;
    deallocate prepare alter_stmt;

    select group_concat(concat('`', column_name, '`') order by ordinal_position)
      into @insert_columns
      from information_schema.columns
     where table_schema = database()
       and table_name = pending_table_name
       and column_name <> 'id';

    select group_concat(
               case
                   when column_name in ('transition_score', 'green_loan_score')
                       then concat('null as `', column_name, '`')
                   else concat('s.`', column_name, '`')
               end
               order by ordinal_position
           )
      into @select_columns
      from information_schema.columns
     where table_schema = database()
       and table_name = pending_table_name
       and column_name <> 'id';

    set @insert_sql = concat(
        'insert into `', pending_table_name, '` (', @insert_columns, ') ',
        'select ', @select_columns, ' ',
        'from `', source_table_name, '` s ',
        'join (',
        '  select coalesce(institution_code, institution_name) as institution_key, max(data_year) as max_year ',
        '  from `', source_table_name, '` ',
        '  group by coalesce(institution_code, institution_name)',
        ') latest ',
        '  on coalesce(s.institution_code, s.institution_name) <=> latest.institution_key ',
        ' and s.data_year = latest.max_year ',
        'order by coalesce(s.institution_code, s.institution_name), s.company_name, s.id'
    );
    prepare insert_stmt from @insert_sql;
    execute insert_stmt;
    deallocate prepare insert_stmt;
end //

delimiter ;

call init_pending_company_data_table('nonferrous_metal_company_data', 'nonferrous_metal_pending_company_data');
call init_pending_company_data_table('power_company_data', 'power_pending_company_data');
call init_pending_company_data_table('steel_company_data', 'steel_pending_company_data');
call init_pending_company_data_table('aviation_airport_company_data', 'aviation_airport_pending_company_data');
call init_pending_company_data_table('basic_chemical_company_data', 'basic_chemical_pending_company_data');
call init_pending_company_data_table('building_material_company_data', 'building_material_pending_company_data');
call init_pending_company_data_table('coal_company_data', 'coal_pending_company_data');
call init_pending_company_data_table('petrochemical_company_data', 'petrochemical_pending_company_data');

drop procedure if exists init_pending_company_data_table;
