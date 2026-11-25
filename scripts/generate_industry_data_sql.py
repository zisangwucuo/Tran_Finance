from __future__ import annotations

from pathlib import Path
from typing import Any

import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_SQL = ROOT / "database" / "20260815_industry_company_data.sql"


COMMON_COLUMNS = {
    "金融机构名称": ("institution_name", "varchar(200)", "金融机构名称"),
    "行业": ("industry_name", "varchar(100)", "行业"),
    "企业名称": ("company_name", "varchar(200)", "企业名称"),
    "股票代码": ("stock_code", "varchar(20)", "股票代码"),
    "企业类型": ("company_type", "varchar(100)", "企业类型"),
    "细分类型": ("sub_industry_type", "varchar(100)", "细分类型"),
    "年份": ("data_year", "int", "年份"),
    "是否_转型目标与承诺": ("transition_target_commitment", "tinyint", "是否具有转型目标与承诺"),
    "是否_转型战略与路径规划": ("transition_strategy_path", "tinyint", "是否具有转型战略与路径规划"),
    "是否_治理架构与激励": ("governance_incentive", "tinyint", "是否具有治理架构与激励"),
    "是否_转型技术应用现状": ("transition_technology_application", "tinyint", "是否具有转型技术应用现状"),
    "是否_碳资产管理能力": ("carbon_asset_management", "tinyint", "是否具有碳资产管理能力"),
    "是否_风险管理与市场机遇": ("risk_management_market_opportunity", "tinyint", "是否具有风险管理与市场机遇"),
    "是否_信息披露与透明度": ("information_disclosure_transparency", "tinyint", "是否具有信息披露与透明度"),
    "是否_公正转型": ("just_transition", "tinyint", "是否具有公正转型"),
    "是否_环境合规": ("environmental_compliance", "tinyint", "是否环境合规"),
    "营业收入(亿元)": ("operating_revenue", "decimal(18,4)", "营业收入，单位亿元"),
    "净利润(亿元)": ("net_profit", "decimal(18,4)", "净利润，单位亿元"),
    "研发费用(亿元)": ("r_and_d_expense", "decimal(18,4)", "研发费用，单位亿元"),
    "资产负债率(%)": ("asset_liability_ratio", "decimal(18,4)", "资产负债率"),
    "经营质量_销售净利率(%)": ("sales_net_profit_margin", "decimal(18,4)", "销售净利率"),
    "经营质量_营业收入增长率(%)": ("revenue_growth_rate", "decimal(18,4)", "营业收入增长率"),
    "经营质量_研发投入强度(%)": ("r_and_d_intensity", "decimal(18,4)", "研发投入强度"),
    "企业经营质量(0-1)": ("operation_quality_score", "decimal(18,4)", "企业经营质量评分"),
    "环保绩效评级": ("environmental_performance_rating", "varchar(30)", "环保绩效评级"),
    "转型综合评分": ("transition_score", "decimal(18,4)", "转型综合评分"),
    "绿色贷款评分": ("green_loan_score", "decimal(18,4)", "绿色贷款评分"),
    "是否绿色贷款": ("green_loan", "tinyint", "目标变量：是否绿色贷款"),
    "是否转型贷款": ("transition_loan", "tinyint", "目标变量：是否转型贷款"),
}


INDUSTRIES = [
    {
        "table": "nonferrous_metal_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_有色金属行业.xlsx",
        "comment": "有色金属行业企业转型金融评估数据表",
        "columns": {
            "电解铝使用可再生能源绿电比例(%)": ("electrolytic_aluminum_renewable_power_ratio", "decimal(18,4)", "电解铝使用可再生能源绿电比例"),
            "电解铝铝液直流电耗(kWh/t-Al)": ("electrolytic_aluminum_dc_power_consumption", "decimal(18,4)", "电解铝铝液直流电耗"),
            "再生金属供应占比(%)": ("recycled_metal_supply_ratio", "decimal(18,4)", "再生金属供应占比"),
            "再生铝产量(万吨)": ("recycled_aluminum_output", "decimal(18,4)", "再生铝产量"),
            "吨铝碳排放强度(tCO2/t-Al)": ("aluminum_carbon_emission_intensity", "decimal(18,4)", "吨铝碳排放强度"),
            "铝水直接合金化比例(%)": ("molten_aluminum_direct_alloying_ratio", "decimal(18,4)", "铝水直接合金化比例"),
        },
    },
    {
        "table": "power_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_电力行业.xlsx",
        "comment": "电力行业企业转型金融评估数据表",
        "columns": {
            "清洁能源可再生能源装机容量占比(%)": ("clean_energy_installed_capacity_ratio", "decimal(18,4)", "清洁能源可再生能源装机容量占比"),
            "清洁能源可再生能源发电量占比(%)": ("clean_energy_generation_ratio", "decimal(18,4)", "清洁能源可再生能源发电量占比"),
            "供电标准煤耗(gce/kWh)": ("power_supply_standard_coal_consumption", "decimal(18,4)", "供电标准煤耗"),
            "单位发电量碳排放强度(tCO2/MWh)": ("power_generation_carbon_intensity", "decimal(18,4)", "单位发电量碳排放强度"),
            "煤电机组灵活性改造比例(%)": ("coal_power_flexibility_retrofit_ratio", "decimal(18,4)", "煤电机组灵活性改造比例"),
            "机组最小技术出力调峰深度(%)": ("minimum_technical_output_peak_depth", "decimal(18,4)", "机组最小技术出力调峰深度"),
            "可再生能源装机投资额(亿元)": ("renewable_energy_investment", "decimal(18,4)", "可再生能源装机投资额"),
        },
    },
    {
        "table": "steel_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_钢铁行业.xlsx",
        "comment": "钢铁行业企业转型金融评估数据表",
        "columns": {
            "高炉工序能耗(kgce/t)": ("blast_furnace_energy_consumption", "decimal(18,4)", "高炉工序能耗"),
            "转炉工序能耗(kgce/t)": ("converter_energy_consumption", "decimal(18,4)", "转炉工序能耗"),
            "电弧炉炼钢工序能耗(kgce/t)": ("electric_arc_furnace_energy_consumption", "decimal(18,4)", "电弧炉炼钢工序能耗"),
            "吨钢碳排放强度(tCO2/tcs)": ("steel_carbon_emission_intensity", "decimal(18,4)", "吨钢碳排放强度"),
            "电炉短流程钢产量占比(%)": ("electric_furnace_short_process_ratio", "decimal(18,4)", "电炉短流程钢产量占比"),
            "废钢利用量(kg/tcs)": ("scrap_steel_use", "decimal(18,4)", "废钢利用量"),
            "高炉转炉焦炉煤气利用率(%)": ("byproduct_gas_utilization_ratio", "decimal(18,4)", "高炉转炉焦炉煤气利用率"),
        },
    },
    {
        "table": "aviation_airport_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_航空机场行业.xlsx",
        "comment": "航空机场行业企业转型金融评估数据表",
        "columns": {
            "运输航空吨公里油耗(kg/t·km)": ("aviation_fuel_consumption_per_ton_km", "decimal(18,4)", "运输航空吨公里油耗"),
            "运输航空吨公里CO2排放(kg/t·km)": ("aviation_co2_emission_per_ton_km", "decimal(18,4)", "运输航空吨公里CO2排放"),
            "单位收入客公里CO2排放(gCO2/RPK)": ("co2_emission_per_rpk", "decimal(18,4)", "单位收入客公里CO2排放"),
            "可持续航空燃料SAF使用量(t)": ("saf_usage", "decimal(18,4)", "可持续航空燃料SAF使用量"),
            "单位旅客吞吐量综合能耗(kgce/pax)": ("passenger_throughput_energy_consumption", "decimal(18,4)", "单位旅客吞吐量综合能耗"),
            "单位旅客吞吐量CO2排放(kgCO2/pax)": ("passenger_throughput_co2_emission", "decimal(18,4)", "单位旅客吞吐量CO2排放"),
            "可再生能源消费占比(%)": ("renewable_energy_consumption_ratio", "decimal(18,4)", "可再生能源消费占比"),
            "地面车辆设备电动化比例(%)": ("ground_vehicle_electrification_ratio", "decimal(18,4)", "地面车辆设备电动化比例"),
        },
    },
    {
        "table": "basic_chemical_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_基础化工行业.xlsx",
        "comment": "基础化工行业企业转型金融评估数据表",
        "columns": {
            "合成氨煤制单位产品综合能耗(kgce/t)": ("synthetic_ammonia_energy_consumption", "decimal(18,4)", "合成氨煤制单位产品综合能耗"),
            "乙烯石脑油裂解单位产品综合能耗(kgce/t)": ("ethylene_naphtha_cracking_energy_consumption", "decimal(18,4)", "乙烯石脑油裂解单位产品综合能耗"),
            "甲醇单位产品综合能耗(kgce/t)": ("methanol_energy_consumption", "decimal(18,4)", "甲醇单位产品综合能耗"),
            "单位产品碳排放强度(tCO2/t)": ("product_carbon_emission_intensity", "decimal(18,4)", "单位产品碳排放强度"),
            "绿色电力使用比例(%)": ("green_power_use_ratio", "decimal(18,4)", "绿色电力使用比例"),
            "绿氢生物质非化石原料使用比例(%)": ("green_hydrogen_biomass_non_fossil_feedstock_ratio", "decimal(18,4)", "绿氢生物质非化石原料使用比例"),
            "工业水重复利用率(%)": ("industrial_water_reuse_ratio", "decimal(18,4)", "工业水重复利用率"),
        },
    },
    {
        "table": "building_material_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_建材行业.xlsx",
        "comment": "建材行业企业转型金融评估数据表",
        "columns": {
            "水泥熟料单位产品综合能耗(kgce/t)": ("clinker_energy_consumption", "decimal(18,4)", "水泥熟料单位产品综合能耗"),
            "平板玻璃单位产品综合能耗(kgce/重量箱)": ("flat_glass_energy_consumption", "decimal(18,4)", "平板玻璃单位产品综合能耗"),
            "单位产品碳排放强度(tCO2/t水泥或玻璃)": ("product_carbon_emission_intensity", "decimal(18,4)", "单位产品碳排放强度"),
            "替代燃料热量替代率TSR(%)": ("alternative_fuel_thermal_substitution_ratio", "decimal(18,4)", "替代燃料热量替代率TSR"),
            "替代原料使用比例(%)": ("alternative_raw_material_use_ratio", "decimal(18,4)", "替代原料使用比例"),
        },
    },
    {
        "table": "coal_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_煤炭行业.xlsx",
        "comment": "煤炭行业企业转型金融评估数据表",
        "columns": {
            "煤矿瓦斯抽采利用率(%)": ("coal_mine_gas_extraction_utilization_ratio", "decimal(18,4)", "煤矿瓦斯抽采利用率"),
            "原煤生产综合能耗(kgce/t)": ("raw_coal_energy_consumption", "decimal(18,4)", "原煤生产综合能耗"),
            "矿井水综合利用率(%)": ("mine_water_reuse_ratio", "decimal(18,4)", "矿井水综合利用率"),
            "煤矸石粉煤灰综合利用率(%)": ("gangue_fly_ash_reuse_ratio", "decimal(18,4)", "煤矸石粉煤灰综合利用率"),
            "非煤产业收入占比(%)": ("non_coal_revenue_ratio", "decimal(18,4)", "非煤产业收入占比"),
            "新能源装机容量(MW)": ("new_energy_installed_capacity", "decimal(18,4)", "新能源装机容量"),
        },
    },
    {
        "table": "petrochemical_company_data",
        "file": r"C:\Users\Administrator\Desktop\转型金融评分模拟数据2.0_石油石化行业.xlsx",
        "comment": "石油石化行业企业转型金融评估数据表",
        "columns": {
            "单位能量因数炼油综合能耗(kg oe/(t·能量因数))": ("refining_energy_consumption_by_energy_factor", "decimal(18,4)", "单位能量因数炼油综合能耗"),
            "单位炼油碳排放强度(tCO2/t原油)": ("refining_carbon_emission_intensity", "decimal(18,4)", "单位炼油碳排放强度"),
            "甲烷排放强度(tCH4/百万桶油当量)": ("methane_emission_intensity", "decimal(18,4)", "甲烷排放强度"),
            "化工品收率化工原料产量占比(%)": ("chemical_product_yield_ratio", "decimal(18,4)", "化工品收率化工原料产量占比"),
            "清洁能源可再生能源使用比例(%)": ("clean_energy_use_ratio", "decimal(18,4)", "清洁能源可再生能源使用比例"),
            "可持续航空燃料生物燃料产量或销量(万吨)": ("sustainable_aviation_fuel_biofuel_output_sales", "decimal(18,4)", "可持续航空燃料生物燃料产量或销量"),
        },
    },
]


def sql_identifier(name: str) -> str:
    return f"`{name}`"


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_value(value: Any, column_name: str) -> str:
    if pd.isna(value):
        return "NULL"
    if column_name == "stock_code":
        if isinstance(value, float) and value.is_integer():
            return sql_string(str(int(value)).zfill(6))
        if isinstance(value, int):
            return sql_string(str(value).zfill(6))
        return sql_string(str(value).strip())
    if isinstance(value, str):
        return sql_string(value.strip())
    if isinstance(value, float):
        if value.is_integer():
            return str(int(value))
        return format(value, ".10g")
    return str(value)


def ordered_column_defs(industry: dict[str, Any]) -> list[tuple[str, str, str, str]]:
    excel_columns = pd.read_excel(industry["file"], nrows=0).columns
    mappings = COMMON_COLUMNS | industry["columns"]
    result = [("institution_code", "varchar(64)", "所属金融机构编码", "institution_code")]
    for column in excel_columns:
        if column not in mappings:
            raise ValueError(f"未配置列映射：{industry['file']} -> {column}")
        db_name, db_type, comment = mappings[column]
        result.append((db_name, db_type, comment, column))
    return result


def create_table_sql(industry: dict[str, Any], columns: list[tuple[str, str, str, str]]) -> str:
    column_lines = [
        "  `id` bigint primary key auto_increment comment '数据ID'",
    ]
    for db_name, db_type, comment, _ in columns:
        nullable = "null"
        column_lines.append(f"  {sql_identifier(db_name)} {db_type} {nullable} comment {sql_string(comment)}")
    column_lines.extend(
        [
            "  key `idx_institution_year` (`institution_code`, `data_year`)",
            "  key `idx_company_name` (`company_name`)",
            "  key `idx_green_loan` (`green_loan`)",
            "  key `idx_transition_loan` (`transition_loan`)",
        ]
    )
    body = ",\n".join(column_lines)
    return (
        f"drop table if exists {sql_identifier(industry['table'])};\n"
        f"create table {sql_identifier(industry['table'])} (\n{body}\n"
        f") engine=InnoDB default charset=utf8mb4 comment={sql_string(industry['comment'])};"
    )


def insert_sql(industry: dict[str, Any], columns: list[tuple[str, str, str, str]]) -> str:
    df = pd.read_excel(industry["file"])
    db_columns = [db_name for db_name, _, _, _ in columns]
    excel_columns = [excel_name for _, _, _, excel_name in columns]
    insert_columns = ", ".join(sql_identifier(column) for column in db_columns)
    lines = []
    for _, row in df.iterrows():
        values = []
        for db_name, excel_name in zip(db_columns, excel_columns):
            if db_name == "institution_code":
                values.append("NULL")
            else:
                values.append(sql_value(row[excel_name], db_name))
        lines.append("(" + ", ".join(values) + ")")
    chunks = []
    for start in range(0, len(lines), 100):
        chunk = ",\n".join(lines[start:start + 100])
        chunks.append(f"insert into {sql_identifier(industry['table'])} ({insert_columns}) values\n{chunk};")
    return "\n".join(chunks)


def main() -> None:
    sql_parts = [
        "-- 2026-08-15 八行业企业转型金融评估数据迁移脚本",
        "-- 执行前请确认 financial_institution 中已经存在银行A、银行B、银行C。",
        "set names utf8mb4;",
        "drop table if exists `electric_company_data`;",
    ]
    for industry in INDUSTRIES:
        columns = ordered_column_defs(industry)
        sql_parts.append(create_table_sql(industry, columns))
        sql_parts.append(insert_sql(industry, columns))
        sql_parts.append(
            f"update {sql_identifier(industry['table'])} data_row "
            "join `financial_institution` institution "
            "on data_row.`institution_name` = institution.`institution_name` "
            "set data_row.`institution_code` = institution.`institution_code`;"
        )
    OUTPUT_SQL.write_text("\n\n".join(sql_parts) + "\n", encoding="utf-8")
    print(f"SQL generated: {OUTPUT_SQL}")


if __name__ == "__main__":
    main()
