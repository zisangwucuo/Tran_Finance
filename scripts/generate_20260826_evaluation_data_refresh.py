"""Generate the 2026-08-26 evaluation data refresh SQL.

The source workbooks are read only. The generated SQL must be reviewed and
executed manually in MySQL after the three financial institutions exist.
"""

from __future__ import annotations

import argparse
import math
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

import pandas as pd


DEFAULT_INPUT_DIR = Path(
    r"C:\Users\Administrator\Desktop\转型金融评分模拟测试数据2023-2025"
    r"\转型金融评分模拟测试数据2023-2025"
)
DEFAULT_OUTPUT = Path(__file__).resolve().parents[1] / "database" / "20260826_evaluation_data_refresh.sql"

COMMON_BOOLEAN_FIELDS = [
    ("transition_target_commitment", "是否_转型目标与承诺"),
    ("transition_strategy_path", "是否_转型战略与路径规划"),
    ("governance_incentive", "是否_治理架构与激励"),
    ("transition_technology_application", "是否_转型技术应用现状"),
    ("carbon_asset_management", "是否_碳资产管理能力"),
    ("risk_management_market_opportunity", "是否_风险管理与市场机遇"),
    ("information_disclosure_transparency", "是否_信息披露与透明度"),
    ("just_transition", "是否_公正转型"),
    ("environmental_compliance", "是否_环境合规"),
]

COMMON_NUMBER_FIELDS = [
    ("sales_net_profit_margin", "经营质量_销售净利率(%)"),
    ("revenue_growth_rate", "经营质量_营业收入增长率(%)"),
    ("r_and_d_intensity", "经营质量_研发投入强度(%)"),
]

INDUSTRY_SPECS = [
    {
        "file": "有色金属行业.xlsx",
        "name": "有色金属行业",
        "history_table": "nonferrous_metal_company_data",
        "pending_table": "nonferrous_metal_pending_company_data",
        "special_fields": [
            ("electrolytic_aluminum_renewable_power_ratio", "电解铝使用可再生能源绿电比例(%)"),
            ("electrolytic_aluminum_dc_power_consumption", "电解铝铝液直流电耗(kWh/t-Al)"),
            ("recycled_metal_supply_ratio", "再生金属供应占比(%)"),
            ("recycled_aluminum_output", "再生铝产量(万吨)"),
            ("aluminum_carbon_emission_intensity", "吨铝碳排放强度(tCO2/t-Al)"),
            ("molten_aluminum_direct_alloying_ratio", "铝水直接合金化比例(%)"),
        ],
    },
    {
        "file": "电力行业.xlsx",
        "name": "电力行业",
        "history_table": "power_company_data",
        "pending_table": "power_pending_company_data",
        "special_fields": [
            ("clean_energy_installed_capacity_ratio", "清洁能源可再生能源装机容量占比(%)"),
            ("clean_energy_generation_ratio", "清洁能源可再生能源发电量占比(%)"),
            ("power_supply_standard_coal_consumption", "供电标准煤耗(gce/kWh)"),
            ("power_generation_carbon_intensity", "单位发电量碳排放强度(tCO2/MWh)"),
            ("coal_power_flexibility_retrofit_ratio", "煤电机组灵活性改造比例(%)"),
            ("minimum_technical_output_peak_depth", "机组最小技术出力调峰深度(%)"),
            ("renewable_energy_investment", "可再生能源装机投资额(亿元)"),
        ],
    },
    {
        "file": "钢铁行业.xlsx",
        "name": "钢铁行业",
        "history_table": "steel_company_data",
        "pending_table": "steel_pending_company_data",
        "special_fields": [
            ("blast_furnace_energy_consumption", "高炉工序能耗(kgce/t)"),
            ("converter_energy_consumption", "转炉工序能耗(kgce/t)"),
            ("electric_arc_furnace_energy_consumption", "电弧炉炼钢工序能耗(kgce/t)"),
            ("steel_carbon_emission_intensity", "吨钢碳排放强度(tCO2/tcs)"),
            ("electric_furnace_short_process_ratio", "电炉短流程钢产量占比(%)"),
            ("scrap_steel_use", "废钢利用量(kg/tcs)"),
            ("byproduct_gas_utilization_ratio", "高炉转炉焦炉煤气利用率(%)"),
        ],
    },
    {
        "file": "航空机场行业.xlsx",
        "name": "航空机场行业",
        "history_table": "aviation_airport_company_data",
        "pending_table": "aviation_airport_pending_company_data",
        "sub_industry_source": "细分类型",
        "special_fields": [
            ("aviation_fuel_consumption_per_ton_km", "运输航空吨公里油耗(kg/t·km)"),
            ("aviation_co2_emission_per_ton_km", "运输航空吨公里CO2排放(kg/t·km)"),
            ("co2_emission_per_rpk", "单位收入客公里CO2排放(gCO2/RPK)"),
            ("saf_usage", "可持续航空燃料SAF使用量(t)"),
            ("passenger_throughput_energy_consumption", "单位旅客吞吐量综合能耗(kgce/pax)"),
            ("passenger_throughput_co2_emission", "单位旅客吞吐量CO2排放(kgCO2/pax)"),
            ("renewable_energy_consumption_ratio", "可再生能源消费占比(%)"),
            ("ground_vehicle_electrification_ratio", "地面车辆设备电动化比例(%)"),
        ],
    },
    {
        "file": "基础化工行业.xlsx",
        "name": "基础化工行业",
        "history_table": "basic_chemical_company_data",
        "pending_table": "basic_chemical_pending_company_data",
        "special_fields": [
            ("synthetic_ammonia_energy_consumption", "合成氨煤制单位产品综合能耗(kgce/t)"),
            ("ethylene_naphtha_cracking_energy_consumption", "乙烯石脑油裂解单位产品综合能耗(kgce/t)"),
            ("methanol_energy_consumption", "甲醇单位产品综合能耗(kgce/t)"),
            ("product_carbon_emission_intensity", "单位产品碳排放强度(tCO2/t)"),
            ("green_power_use_ratio", "绿色电力使用比例(%)"),
            ("green_hydrogen_biomass_non_fossil_feedstock_ratio", "绿氢生物质非化石原料使用比例(%)"),
            ("industrial_water_reuse_ratio", "工业水重复利用率(%)"),
        ],
    },
    {
        "file": "建材行业.xlsx",
        "name": "建材行业",
        "history_table": "building_material_company_data",
        "pending_table": "building_material_pending_company_data",
        "special_fields": [
            ("clinker_energy_consumption", "水泥熟料单位产品综合能耗(kgce/t)"),
            ("flat_glass_energy_consumption", "平板玻璃单位产品综合能耗(kgce/重量箱)"),
            ("product_carbon_emission_intensity", "单位产品碳排放强度(tCO2/t水泥或玻璃)"),
            ("alternative_fuel_thermal_substitution_ratio", "替代燃料热量替代率TSR(%)"),
            ("alternative_raw_material_use_ratio", "替代原料使用比例(%)"),
        ],
    },
    {
        "file": "煤炭行业.xlsx",
        "name": "煤炭行业",
        "history_table": "coal_company_data",
        "pending_table": "coal_pending_company_data",
        "special_fields": [
            ("coal_mine_gas_extraction_utilization_ratio", "煤矿瓦斯抽采利用率(%)"),
            ("raw_coal_energy_consumption", "原煤生产综合能耗(kgce/t)"),
            ("mine_water_reuse_ratio", "矿井水综合利用率(%)"),
            ("gangue_fly_ash_reuse_ratio", "煤矸石粉煤灰综合利用率(%)"),
            ("non_coal_revenue_ratio", "非煤产业收入占比(%)"),
            ("new_energy_installed_capacity", "新能源装机容量(MW)"),
        ],
    },
    {
        "file": "石油石化行业.xlsx",
        "name": "石油石化行业",
        "history_table": "petrochemical_company_data",
        "pending_table": "petrochemical_pending_company_data",
        "special_fields": [
            ("refining_energy_consumption_by_energy_factor", "单位能量因数炼油综合能耗(kg oe/(t·能量因数))"),
            ("refining_carbon_emission_intensity", "单位炼油碳排放强度(tCO2/t原油)"),
            ("methane_emission_intensity", "甲烷排放强度(tCH4/百万桶油当量)"),
            ("chemical_product_yield_ratio", "化工品收率化工原料产量占比(%)"),
            ("clean_energy_use_ratio", "清洁能源可再生能源使用比例(%)"),
            ("sustainable_aviation_fuel_biofuel_output_sales", "可持续航空燃料生物燃料产量或销量(万吨)"),
        ],
    },
]


def sql_identifier(value: str) -> str:
    return value.replace("`", "``")


def is_null(value: Any) -> bool:
    if value is None:
        return True
    if isinstance(value, float) and math.isnan(value):
        return True
    result = pd.isna(value)
    return bool(result) if isinstance(result, bool) else False


def sql_literal(value: Any) -> str:
    if is_null(value):
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        return decimal_literal(value)
    if isinstance(value, Decimal):
        return decimal_literal(value)
    text = str(value).strip()
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def decimal_literal(value: Any) -> str:
    try:
        number = Decimal(str(value))
    except (InvalidOperation, ValueError):
        return sql_literal(str(value))
    if not number.is_finite():
        return "NULL"
    text = format(number, "f")
    if "." in text:
        text = text.rstrip("0").rstrip(".")
    return text or "0"


def normalize_company_type(value: Any) -> str:
    text = "" if is_null(value) else str(value).strip()
    if text == "上市公司":
        return "上市企业"
    if text == "非上市公司":
        return "非上市企业"
    return text


def institution_expression(bank_name: Any) -> str:
    return (
        "(select institution_code from financial_institution "
        f"where institution_name = {sql_literal(bank_name)} limit 1)"
    )


def read_workbook(input_dir: Path, spec: dict[str, Any]) -> pd.DataFrame:
    path = input_dir / spec["file"]
    if not path.is_file():
        raise FileNotFoundError(f"Workbook not found: {path}")
    frame = pd.read_excel(path, sheet_name=0)
    frame.columns = [str(column).strip() for column in frame.columns]
    required = {
        "银行",
        "行业",
        "企业名称",
        "企业类型",
        "年份",
        "环保绩效评级",
        *(column for _, column in COMMON_NUMBER_FIELDS),
        *(column for _, column in spec["special_fields"]),
    }
    required.update(
        column
        for _, column in COMMON_BOOLEAN_FIELDS
        if column != "是否_环境合规" or spec["name"] != "航空机场行业"
    )
    if spec.get("sub_industry_source"):
        required.add(spec["sub_industry_source"])
    missing = sorted(required.difference(frame.columns))
    if missing:
        raise ValueError(f"{spec['file']} is missing columns: {missing}")
    return frame


def table_definition(spec: dict[str, Any]) -> list[str]:
    columns = [
        "  `id` bigint primary key auto_increment comment 'data id'",
        "  `institution_code` varchar(64) not null comment 'financial institution code'",
        "  `institution_name` varchar(200) not null comment 'financial institution name'",
        "  `industry_name` varchar(100) not null comment 'industry name'",
        "  `company_name` varchar(200) not null comment 'company name'",
        "  `stock_code` varchar(64) null comment 'stock code, always null in this refresh'",
        "  `company_type` varchar(30) not null comment 'listed or non-listed company'",
    ]
    if spec.get("sub_industry_source"):
        columns.append("  `sub_industry_type` varchar(50) null comment 'sub-industry type'")
    columns.extend(
        [
            "  `data_year` int not null comment 'data year'",
            *[
                f"  `{column}` tinyint null comment 'boolean indicator'"
                for column, _ in COMMON_BOOLEAN_FIELDS
            ],
            *[
                f"  `{column}` decimal(18,6) null comment 'common numeric indicator'"
                for column, _ in COMMON_NUMBER_FIELDS
            ],
            *[
                f"  `{column}` decimal(18,6) null comment 'industry-specific indicator'"
                for column, _ in spec["special_fields"]
            ],
            "  `environmental_performance_rating` varchar(30) null comment 'environmental performance rating'",
            "  `transition_score` decimal(18,4) null comment 'transition loan score'",
            "  key `idx_pending_institution_year` (`institution_code`, `data_year`)",
            "  key `idx_pending_company_name` (`company_name`)",
        ]
    )
    return [
        line + "," if index < len(columns) - 1 else line
        for index, line in enumerate(columns)
    ]


def insert_columns(spec: dict[str, Any]) -> list[str]:
    columns = [
        "institution_code",
        "institution_name",
        "industry_name",
        "company_name",
        "stock_code",
        "company_type",
    ]
    if spec.get("sub_industry_source"):
        columns.append("sub_industry_type")
    columns.extend(["data_year"])
    columns.extend(column for column, _ in COMMON_BOOLEAN_FIELDS)
    columns.extend(column for column, _ in COMMON_NUMBER_FIELDS)
    columns.extend(column for column, _ in spec["special_fields"])
    columns.extend(["environmental_performance_rating", "transition_score"])
    return columns


def row_values(row: pd.Series, spec: dict[str, Any]) -> list[str]:
    values = [
        institution_expression(row["银行"]),
        sql_literal(row["银行"]),
        sql_literal(spec["name"]),
        sql_literal(row["企业名称"]),
        "NULL",
        sql_literal(normalize_company_type(row["企业类型"])),
    ]
    if spec.get("sub_industry_source"):
        values.append(sql_literal(row[spec["sub_industry_source"]]))
    values.append(sql_literal(int(row["年份"])))
    values.extend(
        sql_literal(row[source] if source in row.index else None)
        for _, source in COMMON_BOOLEAN_FIELDS
    )
    values.extend(sql_literal(row[source]) for _, source in COMMON_NUMBER_FIELDS)
    values.extend(sql_literal(row[source]) for _, source in spec["special_fields"])
    values.extend([sql_literal(row["环保绩效评级"]), "NULL"])
    return values


def append_batched_inserts(lines: list[str], frame: pd.DataFrame, spec: dict[str, Any]) -> None:
    columns = ", ".join(f"`{sql_identifier(column)}`" for column in insert_columns(spec))
    batch_size = 50
    rows = list(frame.iterrows())
    for start in range(0, len(rows), batch_size):
        batch = rows[start : start + batch_size]
        lines.append(f"insert into `{sql_identifier(spec['pending_table'])}` ({columns}) values")
        values_sql = []
        for _, row in batch:
            values_sql.append("    (" + ", ".join(row_values(row, spec)) + ")")
        lines.append(",\n".join(values_sql) + ";")
        lines.append("")


def migration_header() -> list[str]:
    return [
        "-- 2026-08-26 evaluation data refresh",
        "-- Review this file and execute it manually after bank_a, bank_b and bank_c exist.",
        "-- This script does not contain database credentials and does not delete local model files.",
        "",
        "use `trans_finance`;",
        "set names utf8mb4;",
        "",
        "-- The old single-table dataset is no longer used by the platform.",
        "drop table if exists `electric_company_data`;",
        "",
        "create table if not exists `model_training_job` (",
        "  `id` bigint primary key auto_increment comment 'training job id',",
        "  `institution_code` varchar(64) not null comment 'financial institution code',",
        "  `job_type` varchar(30) not null comment 'train_all/update',",
        "  `status` varchar(30) not null comment 'PENDING/RUNNING/SUCCESS/FAILED',",
        "  `progress` int not null default 0 comment 'training progress',",
        "  `message` varchar(500) null comment 'job message',",
        "  `total_models` int not null default 0 comment 'planned model count',",
        "  `trained_models` int not null default 0 comment 'trained model count',",
        "  `started_at` datetime null comment 'start time',",
        "  `finished_at` datetime null comment 'finish time',",
        "  `created_at` datetime not null default current_timestamp comment 'create time',",
        "  key `idx_training_job_institution` (`institution_code`, `created_at`)",
        ") engine=InnoDB default charset=utf8mb4 comment='model training jobs';",
        "",
        "create table if not exists `industry_model_metadata` (",
        "  `id` bigint primary key auto_increment comment 'model metadata id',",
        "  `institution_code` varchar(64) not null comment 'financial institution code',",
        "  `industry_key` varchar(64) not null comment 'industry model key',",
        "  `industry_name` varchar(100) not null comment 'industry name',",
        "  `target_name` varchar(64) not null comment 'transition_loan only',",
        "  `model_type` varchar(100) not null comment 'model type',",
        "  `model_path` varchar(500) not null comment 'model file path',",
        "  `feature_columns_json` json null comment 'feature columns',",
        "  `feature_weights_json` json null comment 'model feature weights',",
        "  `metrics_json` json null comment 'training metrics',",
        "  `sample_count` int not null default 0 comment 'sample count',",
        "  `company_count` int not null default 0 comment 'company count',",
        "  `year_start` int null comment 'training start year',",
        "  `year_end` int null comment 'training end year',",
        "  `model_version` varchar(64) not null comment 'model version',",
        "  `status` varchar(30) not null comment 'model status',",
        "  `trained_at` datetime null comment 'trained time',",
        "  `updated_at` datetime not null default current_timestamp on update current_timestamp comment 'updated time',",
        "  unique key `uk_model_scope` (`institution_code`, `industry_key`, `target_name`),",
        "  key `idx_model_institution` (`institution_code`, `industry_key`)",
        ") engine=InnoDB default charset=utf8mb4 comment='industry model metadata';",
        "",
        "drop procedure if exists `add_column_if_missing`;",
        "delimiter //",
        "create procedure `add_column_if_missing`(in p_table_name varchar(128), in p_column_name varchar(128), in p_definition varchar(500))",
        "begin",
        "  if not exists (",
        "      select 1 from information_schema.columns",
        "       where table_schema = database()",
        "         and table_name = p_table_name",
        "         and column_name = p_column_name",
        "  ) then",
        "    set @alter_sql = concat('alter table `', replace(p_table_name, '`', '``'), '` add column `',",
        "                            replace(p_column_name, '`', '``'), '` ', p_definition);",
        "    prepare alter_stmt from @alter_sql;",
        "    execute alter_stmt;",
        "    deallocate prepare alter_stmt;",
        "  end if;",
        "end //",
        "delimiter ;",
        "",
        "call `add_column_if_missing`('industry_model_metadata', 'feature_weights_json', 'json null comment ''model feature weights'' after feature_columns_json');",
        "drop procedure if exists `add_column_if_missing`;",
        "",
        "-- Existing model metadata is incompatible with the current industry model contract.",
        "delete from `industry_model_metadata`;",
        "delete from `model_training_job`;",
        "",
        "drop procedure if exists `drop_column_if_exists`;",
        "delimiter //",
        "create procedure `drop_column_if_exists`(in p_table_name varchar(128), in p_column_name varchar(128))",
        "begin",
        "  if exists (",
        "      select 1 from information_schema.columns",
        "       where table_schema = database()",
        "         and table_name = p_table_name",
        "         and column_name = p_column_name",
        "  ) then",
        "    set @drop_sql = concat('alter table `', replace(p_table_name, '`', '``'), '` drop column `',",
        "                           replace(p_column_name, '`', '``'), '`');",
        "    prepare drop_stmt from @drop_sql;",
        "    execute drop_stmt;",
        "    deallocate prepare drop_stmt;",
        "  end if;",
        "end //",
        "delimiter ;",
        "",
    ]


def generate_sql(input_dir: Path) -> str:
    lines = migration_header()

    for spec in INDUSTRY_SPECS:
        for column in [
            "operating_revenue",
            "net_profit",
            "r_and_d_expense",
            "asset_liability_ratio",
            "operation_quality_score",
            "green_loan_score",
            "green_loan",
        ]:
            lines.append(
                f"call `drop_column_if_exists`('{spec['history_table']}', '{column}');"
            )
    lines.extend(
        [
            "drop procedure if exists `drop_column_if_exists`;",
            "",
            "-- Old reminders refer to the removed indicator set and must not be reused.",
            "drop procedure if exists `clear_table_if_exists`;",
            "delimiter //",
            "create procedure `clear_table_if_exists`(in p_table_name varchar(128))",
            "begin",
            "  if exists (",
            "      select 1 from information_schema.tables",
            "       where table_schema = database()",
            "         and table_name = p_table_name",
            "  ) then",
            "    set @clear_sql = concat('delete from `', replace(p_table_name, '`', '``'), '`');",
            "    prepare clear_stmt from @clear_sql;",
            "    execute clear_stmt;",
            "    deallocate prepare clear_stmt;",
            "  end if;",
            "end //",
            "delimiter ;",
            "",
            "call `clear_table_if_exists`('company_data_missing_reminder');",
            "call `clear_table_if_exists`('company_data_change_notice');",
            "drop procedure if exists `clear_table_if_exists`;",
            "",
        ]
    )

    for spec in INDUSTRY_SPECS:
        lines.extend(
            [
                f"drop table if exists `{sql_identifier(spec['pending_table'])}`;",
                f"create table `{sql_identifier(spec['pending_table'])}` (",
                *table_definition(spec),
                ") engine=InnoDB default charset=utf8mb4 comment='pending evaluation data';",
                "",
            ]
        )
        frame = read_workbook(input_dir, spec)
        append_batched_inserts(lines, frame, spec)

    lines.extend(
        [
            "-- Pending table ids are allocated from 1 because each table is rebuilt above.",
            "analyze table",
            "  `nonferrous_metal_pending_company_data`,",
            "  `power_pending_company_data`,",
            "  `steel_pending_company_data`,",
            "  `aviation_airport_pending_company_data`,",
            "  `basic_chemical_pending_company_data`,",
            "  `building_material_pending_company_data`,",
            "  `coal_pending_company_data`,",
            "  `petrochemical_pending_company_data`;",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, default=DEFAULT_INPUT_DIR)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    sql = generate_sql(args.input_dir)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(sql, encoding="utf-8")
    print(f"Generated {args.output} ({len(sql):,} characters)")


if __name__ == "__main__":
    main()
