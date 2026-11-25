"""只读回归验证：用MySQL CTE构造身份边界案例，不写入或修改业务数据。"""
import os
from pathlib import Path
import pymysql
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parent.parent


def main():
    # 直接读取生产查询表达式，防止测试使用另一套规则而漏掉SQL兼容性问题。
    expression = (ROOT / "backend/src/main/java/com/transfinance/support/CompanyIdentitySql.java").read_text(encoding="utf-8").split('"""')[1]
    env = {**dotenv_values(ROOT / "ml-service/.env.local"), **os.environ}
    cases = [
        (None, [("one", "600236", "power")], "one"),
        ("-", [("one", "600236", "power")], "one"),
        (" ", [("one", "600236", "power")], "one"),
        ("600236", [("one", None, "power")], "one"),
        ("600236", [("one", "600236", "power"), ("two", "600237", "power")], "one"),
        (None, [("one", "600236", "power"), ("two", "600237", "power")], None),
        ("600238", [("one", "600236", "power")], None),
        (None, [("one", "600236", "coal")], None),
        ("600236", [("one", "600236", "power"), ("two", "600236", "power")], None),
    ]
    with pymysql.connect(host=env.get("DB_HOST", "127.0.0.1"), port=int(env.get("DB_PORT", 3306)),
                         user=env.get("DB_USERNAME", "root"), password=env.get("DB_PASSWORD", ""),
                         database=env.get("DB_NAME", "trans_finance"), charset="utf8mb4") as conn:
        with conn.cursor() as cur:
            for stock, profiles, expected in cases:
                parts, params = [], []
                for code, profile_stock, industry in profiles:
                    parts.append("SELECT %s company_code, 'example' company_name, %s stock_code, %s industry_name")
                    params.extend([code, profile_stock, industry])
                sql = "WITH company AS (" + " UNION ALL ".join(parts) + "), t AS (SELECT 'example' company_name, 'power' industry_name, %s stock_code) SELECT " + expression + " FROM t"
                cur.execute(sql, [*params, stock])
                assert cur.fetchone()[0] == expected, "Identity resolution regression"
            # 覆盖真实基础表完整投影：编码表达式在股票代码之前，复现此前
            # HAVING引用外层字段触发forward reference的实际列表场景。
            cur.execute("SHOW TABLES")
            tables = [row[0] for row in cur.fetchall()
                      if row[0].endswith("_company_data") and row[0] != "electric_company_data"]
            checked = 0
            for table in tables:
                if not table.replace("_", "").isalnum():
                    raise ValueError("Unexpected table identifier")
                cur.execute(f"SHOW COLUMNS FROM `{table}`")
                columns = [row[0] for row in cur.fetchall()]
                if not {"company_name", "stock_code", "industry_name", "institution_code", "data_year"}.issubset(columns):
                    continue
                projection = expression + " AS companyCode, " + ", ".join(f"t.`{col}`" for col in columns)
                cur.execute(f"SELECT {projection} FROM `{table}` t WHERE t.institution_code=%s ORDER BY t.data_year DESC, t.id DESC LIMIT 10", ("bank_b",))
                cur.fetchall()
                cur.execute(f"SELECT COUNT(*) FROM `{table}` t WHERE t.institution_code=%s AND {expression} LIKE %s", ("bank_b", "%wndl%"))
                cur.fetchone()
                cur.execute(f"SELECT t.data_year, t.transition_score FROM `{table}` t WHERE t.institution_code=%s AND {expression}=%s AND t.transition_score IS NOT NULL ORDER BY t.data_year DESC LIMIT 3", ("bank_b", "wndl"))
                cur.fetchall()
                checked += 1
            assert checked >= 16, "Expected eight history and eight pending tables"
    print(f"PASS: {checked} real tables: full projection, code filter and score trend")
    print(f"PASS: {len(cases)} MySQL identity boundary cases; no business data changed")


if __name__ == "__main__":
    main()
