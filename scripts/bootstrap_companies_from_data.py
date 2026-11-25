import os
import random
import subprocess
from dataclasses import dataclass


AREA_CODES = [
    45217, 45253, 45761, 46318, 46826, 47010, 47297, 47614, 48119, 49062, 49324, 49896,
    50371, 50614, 50622, 50906, 51387, 51446, 52218, 52698, 52980, 53689, 54481, 55290
]
INITIALS = list("abcdefghjklmnopqrstwxyz")


@dataclass(frozen=True)
class IndustryTable:
    key: str
    name: str
    table_name: str

    @property
    def pending_table_name(self) -> str:
        return self.table_name.replace("_company_data", "_pending_company_data")


INDUSTRIES = [
    IndustryTable("nonferrous", "有色金属行业", "nonferrous_metal_company_data"),
    IndustryTable("power", "电力行业", "power_company_data"),
    IndustryTable("steel", "钢铁行业", "steel_company_data"),
    IndustryTable("aviation", "航空机场行业", "aviation_airport_company_data"),
    IndustryTable("chemical", "基础化工行业", "basic_chemical_company_data"),
    IndustryTable("building", "建材行业", "building_material_company_data"),
    IndustryTable("coal", "煤炭行业", "coal_company_data"),
    IndustryTable("petrochemical", "石油石化行业", "petrochemical_company_data"),
]


def mysql_args() -> list[str]:
    host = os.environ.get("DB_HOST", "localhost")
    port = os.environ.get("DB_PORT", "3306")
    user = os.environ.get("DB_USERNAME", "root")
    database = os.environ.get("DB_NAME", "trans_finance")
    return [
        "mysql",
        f"-h{host}",
        f"-P{port}",
        f"-u{user}",
        "--default-character-set=utf8mb4",
        "-N",
        "-B",
        database,
    ]


def run_query(sql: str) -> list[list[str]]:
    result = subprocess.run(
        mysql_args() + ["-e", sql],
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    rows = []
    for line in result.stdout.splitlines():
        if line.strip():
            rows.append([value if value != "NULL" else "" for value in line.split("\t")])
    return rows


def run_statements(sql: str) -> None:
    if not sql.strip():
        return
    subprocess.run(
        mysql_args(),
        input=sql,
        check=True,
        text=True,
        encoding="utf-8",
    )


def sql_quote(value: str | None) -> str:
    if value is None:
        return "null"
    escaped = value.replace("\\", "\\\\").replace("'", "\\'")
    return f"'{escaped}'"


def normalize_stock_code(value: str | None) -> str:
    value = (value or "").strip()
    return value if value else "-"


def normalize_company_type(value: str | None) -> str:
    value = (value or "").strip()
    if value == "上市公司":
        return "上市企业"
    if value == "非上市公司":
        return "非上市企业"
    return value or "上市企业"


def initials(text: str) -> str:
    chars = []
    for value in text:
        if value.isascii() and value.isalnum():
            chars.append(value.lower())
        elif "\u4e00" <= value <= "\u9fff":
            chars.append(pinyin_initial(value))
    code = "".join(chars)
    code = "".join(value for value in code if value.isalnum())
    return code or "company"


def pinyin_initial(value: str) -> str:
    try:
        data = value.encode("gb2312")
        if len(data) < 2:
            return "x"
        code = data[0] * 256 + data[1]
        for index in range(len(AREA_CODES) - 1, -1, -1):
            if code >= AREA_CODES[index]:
                return INITIALS[index]
    except Exception:
        return "x"
    return "x"


def unique_code(company_name: str, used_codes: set[str]) -> str:
    base_code = initials(company_name)
    if base_code not in used_codes:
        used_codes.add(base_code)
        return base_code
    for _ in range(20):
        next_code = f"{base_code}_{random.randint(0, 9999)}"
        if next_code not in used_codes:
            used_codes.add(next_code)
            return next_code
    suffix = 1
    while f"{base_code}_{suffix}" in used_codes:
        suffix += 1
    next_code = f"{base_code}_{suffix}"
    used_codes.add(next_code)
    return next_code


def unique_username(base_username: str, used_usernames: set[str]) -> str:
    if base_username not in used_usernames:
        used_usernames.add(base_username)
        return base_username
    for _ in range(20):
        username = f"{base_username}_{random.randint(0, 9999)}"
        if username not in used_usernames:
            used_usernames.add(username)
            return username
    suffix = 1
    while f"{base_username}_{suffix}" in used_usernames:
        suffix += 1
    username = f"{base_username}_{suffix}"
    used_usernames.add(username)
    return username


def main() -> None:
    password = os.environ.get("COMPANY_DEFAULT_PASSWORD")
    if not password:
        raise SystemExit("请先设置 COMPANY_DEFAULT_PASSWORD 环境变量。")

    existing_companies = {}
    used_codes = set()
    for company_code, company_name, industry_key, stock_code in run_query(
        "select company_code, company_name, industry_key, coalesce(stock_code, '') from company"
    ):
        used_codes.add(company_code)
        key = (company_name, industry_key, normalize_stock_code(stock_code))
        if key in existing_companies:
            raise SystemExit("企业主档存在重复身份，请先执行身份核查，未写入数据。")
        existing_companies[key] = company_code

    used_usernames = set()
    company_codes_with_user = set()
    for username, company_code in run_query("select username, company_code from company_user"):
        used_usernames.add(username)
        company_codes_with_user.add(company_code)

    rows_to_insert = []
    user_rows_to_insert = []
    for industry in INDUSTRIES:
        for table_name in [industry.table_name, industry.pending_table_name]:
            rows = run_query(
                "select distinct company_name, coalesce(stock_code, ''), coalesce(company_type, ''), "
                f"coalesce(industry_name, '') from `{table_name}` "
                "where company_name is not null and company_name <> ''"
            )
            for company_name, stock_code, company_type, industry_name in rows:
                normalized_stock_code = normalize_stock_code(stock_code)
                key = (company_name, industry.key, normalized_stock_code)
                company_code = existing_companies.get(key)
                candidates = [(k, v) for k, v in existing_companies.items()
                              if k[:2] == (company_name, industry.key)]
                # 缺少股票代码不代表新企业；同名同行业唯一时复用，歧义时停止。
                if normalized_stock_code == "-" and candidates:
                    if len(candidates) != 1:
                        raise SystemExit("企业身份存在歧义，未写入数据，请先核实企业主档。")
                    company_code = candidates[0][1]
                elif company_code is None and candidates:
                    if len(candidates) == 1 and candidates[0][0][2] == "-":
                        company_code = candidates[0][1]
                    else:
                        raise SystemExit("企业股票代码存在冲突，未写入数据，请先核实企业主档。")
                if company_code is None:
                    company_code = unique_code(company_name, used_codes)
                    existing_companies[key] = company_code
                    rows_to_insert.append((
                        company_code,
                        company_name,
                        normalize_company_type(company_type),
                        normalized_stock_code,
                        industry.key,
                        industry_name or industry.name,
                    ))
                if company_code not in company_codes_with_user:
                    username = unique_username(company_code, used_usernames)
                    company_codes_with_user.add(company_code)
                    user_rows_to_insert.append((company_code, username, password))

    statements = ["set names utf8mb4;"]
    for company_code, company_name, company_type, stock_code, industry_key, industry_name in rows_to_insert:
        statements.append(
            "insert into company "
            "(company_code, company_name, company_type, stock_code, industry_key, industry_name) values "
            f"({sql_quote(company_code)}, {sql_quote(company_name)}, {sql_quote(company_type)}, "
            f"{sql_quote(stock_code)}, {sql_quote(industry_key)}, {sql_quote(industry_name)});"
        )
    for company_code, username, user_password in user_rows_to_insert:
        statements.append(
            "insert into company_user (company_code, username, password_hash) values "
            f"({sql_quote(company_code)}, {sql_quote(username)}, {sql_quote(user_password)});"
        )

    run_statements("\n".join(statements))
    print(f"新增企业 {len(rows_to_insert)} 条，新增企业用户 {len(user_rows_to_insert)} 条。")


if __name__ == "__main__":
    main()
