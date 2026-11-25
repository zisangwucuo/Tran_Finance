"""建立单机三机构只读边界，不复制或修改原始业务记录。

由本机管理员运行一次，生成三个独立MySQL账号和仅本机构可见的视图。
账号密码和节点凭据使用secrets生成，保存到已限制访问且被Git忽略的运行目录。
运行节点不会继承管理员数据库密码。重复执行只更新视图，不重置已有凭据。
"""
import ast
import json
import os
import re
import secrets
from pathlib import Path

import pymysql
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / ".runtime" / "psi"
BANKS = ("bank_a", "bank_b", "bank_c")


def catalog():
    # 使用Python AST读取现有模型服务字段常量，不导入模型服务或触发训练逻辑。
    constants = {}
    names = {"INDUSTRIES", "COMMON_FEATURES", "SPECIAL_FEATURES", "ENVIRONMENTAL_RATING_FEATURE"}
    tree = ast.parse((ROOT / "ml-service" / "app.py").read_text(encoding="utf-8-sig"))
    for node in tree.body:
        if isinstance(node, ast.Assign) and len(node.targets) == 1 and isinstance(node.targets[0], ast.Name):
            if node.targets[0].id in names:
                constants[node.targets[0].id] = ast.literal_eval(node.value)
    result = {}
    for key, (name, table) in constants["INDUSTRIES"].items():
        features = constants["COMMON_FEATURES"] + constants["SPECIAL_FEATURES"][key] + [constants["ENVIRONMENTAL_RATING_FEATURE"]]
        fields = [{"column": "company_type", "type": "text"}]
        fields += [{"column": f[1], "type": "number" if f[2] == "range" else "text" if f[2] == "rating" else f[2]} for f in features]
        if key == "aviation":
            fields.append({"column": "sub_industry_type", "type": "text"})
        result[key] = {"name": name, "history": table, "pending": table.replace("_company_data", "_pending_company_data"), "fields": fields}
    return result


def identifier(value):
    if not re.fullmatch(r"[A-Za-z0-9_]+", value):
        raise ValueError("Invalid database identifier")
    return "`" + value + "`"


def save(path, value):
    temp = path.with_suffix(".tmp")
    temp.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    temp.replace(path)


def provision():
    if not RUNTIME.is_dir():
        raise ValueError("Use scripts/start-psi-nodes.ps1 to prepare protected runtime directory")
    env = {**dotenv_values(ROOT / "ml-service" / ".env.local"), **os.environ}
    if env.get("DB_HOST", "127.0.0.1") not in ("127.0.0.1", "localhost"):
        raise ValueError("Windows PSI simulation requires a local MySQL server")
    source = env.get("DB_NAME", "trans_finance")
    source_sql = identifier(source)
    db = pymysql.connect(host=env.get("DB_HOST", "127.0.0.1"), port=int(env.get("DB_PORT", 3306)),
            user=env.get("DB_USERNAME", "root"), password=env.get("DB_PASSWORD", ""),
            database=source, charset="utf8mb4", autocommit=True)
    industries = catalog()
    plan_file = RUNTIME / "provision.json"
    if plan_file.exists():
        plan = json.loads(plan_file.read_text(encoding="utf-8"))
        if plan["source"] != source:
            raise ValueError("PSI runtime belongs to another database")
    else:
        plan = {"source": source, "banks": {}, "pairs": {}}
        for bank in BANKS:
            plan["banks"][bank] = {"schema": "tf_psi_" + bank,
                    "user": "tf_psi_" + bank + "_" + secrets.token_hex(3),
                    "password": secrets.token_urlsafe(32), "adminToken": secrets.token_urlsafe(32)}
        for i, a in enumerate(BANKS):
            for b in BANKS[i + 1:]:
                plan["pairs"][a + ":" + b] = secrets.token_urlsafe(32)
        # 先保存恢复信息，DDL中断后重试仍使用同一组随机账号。
        save(plan_file, plan)
    gateway = {"nodes": {}}
    with db:
        with db.cursor() as cur:
            cur.execute("SELECT institution_code,institution_name FROM financial_institution WHERE institution_code IN (%s,%s,%s)", BANKS)
            bank_names = dict(cur.fetchall())
            if set(bank_names) != set(BANKS):
                raise ValueError("Three bank institutions are required")
            for index, bank in enumerate(BANKS):
                settings = plan["banks"][bank]
                schema = identifier(settings["schema"])
                cur.execute(f"CREATE DATABASE IF NOT EXISTS {schema} CHARACTER SET utf8mb4")
                # 本机模拟只创建localhost账号，禁止扩大为任意远程来源。
                cur.execute("CREATE USER IF NOT EXISTS %s@'localhost' IDENTIFIED BY %s", (settings["user"], settings["password"]))
                for key, meta in industries.items():
                    for dataset in ("history", "pending"):
                        table = identifier(meta[dataset])
                        columns = ["id", "company_name", "data_year"] + [f["column"] for f in meta["fields"]]
                        selected = ",".join("t." + identifier(c) for c in columns)
                        # 只接受唯一企业映射。股票代码缺失时仍要求同名同行业唯一。
                        identity = f"""(SELECT CASE WHEN COUNT(*)=1 THEN MIN(c.company_code) ELSE NULL END
                            FROM {source_sql}.company c WHERE c.company_name=t.company_name
                            AND c.industry_key=%s AND (COALESCE(NULLIF(t.stock_code,'-'),'')=''
                            OR c.stock_code=t.stock_code)) AS psi_company_code"""
                        cur.execute(f"CREATE OR REPLACE SQL SECURITY DEFINER VIEW {schema}.{table} AS SELECT {selected},{identity}"
                                    f" FROM {source_sql}.{table} t WHERE t.institution_code=%s", (key, bank))
                # 本节点只能读自己的视图schema，不能访问源表或其他银行schema。
                cur.execute(f"GRANT SELECT ON {schema}.* TO %s@'localhost'", (settings["user"],))
                config = {"institutionId": bank, "port": 8111 + index, "adminToken": settings["adminToken"], "industries": industries,
                          "database": {"host": env.get("DB_HOST", "127.0.0.1"),
                                       "port": int(env.get("DB_PORT", 3306)), "user": settings["user"],
                                       "password": settings["password"], "database": settings["schema"]}, "peers": {}}
                for peer in BANKS:
                    if peer == bank:
                        continue
                    config["peers"][peer] = {"url": "http://127.0.0.1:" + str(8111 + BANKS.index(peer)), "name": bank_names[peer],
                                             "token": plan["pairs"][":".join(sorted((bank, peer))) ]}
                save(RUNTIME / (bank + ".json"), config)
                port = 8111 + index
                gateway["nodes"][bank] = {"url": "http://127.0.0.1:" + str(port), "token": settings["adminToken"], "name": bank_names[bank]}
    save(RUNTIME / "gateway.json", gateway)
    print("PSI node views and credentials prepared; business data unchanged.")


if __name__ == "__main__":
    try:
        provision()
    except Exception as exc:
        # 异常消息可能含连接信息，因此只输出类型。
        print("PSI provisioning failed: " + type(exc).__name__)
        raise SystemExit(1) from None
