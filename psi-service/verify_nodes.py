"""本地验收脚本：检查只读权限、鉴权和实际PSI结果，不输出凭据或企业明细。

管理员连接只在本验收脚本中用于对照真实集合，不在银行节点运行路径中使用。
脚本不新增、更改或删除业务数据；写权限检查使用WHERE 1=0且必须被数据库拒绝。
"""
import json
import os
import time
import urllib.request
import urllib.error
from pathlib import Path
import pymysql
from dotenv import dotenv_values
from records import prepare, inputs, summarize

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / ".runtime" / "psi"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def call(config, path, body=None, token=None):
    req = urllib.request.Request("http://127.0.0.1:" + str(config["port"]) + path,
            data=None if body is None else json.dumps(body).encode(),
            headers={"Content-Type": "application/json", "Authorization": "Bearer " + (token or config["adminToken"])})
    with OPENER.open(req, timeout=210) as response:
        return json.load(response)


def main():
    configs = {bank: json.loads((RUNTIME / (bank + ".json")).read_text(encoding="utf-8"))
               for bank in ("bank_a", "bank_b", "bank_c")}
    env = {**dotenv_values(ROOT / "ml-service" / ".env.local"), **os.environ}
    source = env.get("DB_NAME", "trans_finance")
    from provision import identifier
    root = pymysql.connect(host=env.get("DB_HOST", "127.0.0.1"), port=int(env.get("DB_PORT", 3306)),
            user=env.get("DB_USERNAME", "root"), password=env.get("DB_PASSWORD", ""),
            database=source, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor)
    local = {}
    with root:
        for bank, config in configs.items():
            assert call(config, "/health")["protocol"] == "MPC4J-1.1.5-HFH99-ECC-FIELDS-v2"
            try:
                call(config, "/health", token="invalid-test-credential")
                raise AssertionError("Unauthenticated access accepted")
            except urllib.error.HTTPError as error:
                assert error.code == 401
            with pymysql.connect(**config["database"], charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor) as conn:
                with conn.cursor() as cur, root.cursor() as admin:
                    for industry, meta in config["industries"].items():
                        for dataset in ("history", "pending"):
                            table = meta[dataset]
                            cur.execute(f"SELECT * FROM {identifier(table)}")
                            rows = cur.fetchall()
                            admin.execute(f"SELECT id FROM {identifier(table)} WHERE institution_code=%s", (bank,))
                            assert {r["id"] for r in rows} == {r["id"] for r in admin.fetchall()}
                            local[bank, industry, dataset] = prepare(rows, industry, dataset, meta["fields"])
                    table = config["industries"]["power"]["history"]
                    other = configs["bank_b" if bank == "bank_a" else "bank_a"]["database"]["database"]
                    forbidden = [f"SELECT * FROM {identifier(source)}.{identifier(table)} LIMIT 1",
                                 f"SELECT * FROM {identifier(other)}.{identifier(table)} LIMIT 1",
                                 f"UPDATE {identifier(table)} SET company_name=company_name WHERE 1=0"]
                    for sql in forbidden:
                        try:
                            cur.execute(sql)
                            raise AssertionError("Isolation boundary failed")
                        except pymysql.err.OperationalError as error:
                            assert error.args[0] in (1044, 1142, 1143)
            print(bank + ": authentication and all 16 views verified", flush=True)
    # 用本地只读快照计算验收基准，逐银行核对实际HTTP加密求交结果。
    for industry in configs["bank_a"]["industries"]:
        for dataset in ("history", "pending"):
            for bank, config in configs.items():
                groups, skipped = local[bank, industry, dataset]
                expected = []
                for peer, meta in config["peers"].items():
                    intersection = set(inputs(groups, True)) & set(inputs(local[peer, industry, dataset][0], False))
                    expected.extend(summarize(groups, intersection, peer, meta["name"]))
                for attempt in range(3):
                    try:
                        actual = call(config, "/run", {"industry": industry, "dataset": dataset})
                        break
                    except urllib.error.HTTPError as error:
                        if error.code != 429 or attempt == 2:
                            raise
                        time.sleep(3)
                canonical = lambda values: sorted(json.dumps(v, sort_keys=True, ensure_ascii=False) for v in values)
                assert canonical(actual["records"]) == canonical(expected)
                assert actual["skippedLocalRows"] == skipped
            print(industry + "/" + dataset + ": three-bank real PSI matches reference", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("Node verification failed: " + type(exc).__name__, flush=True)
        raise SystemExit(1) from None
