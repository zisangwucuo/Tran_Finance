"""修复缺股票代码导致的重复企业。默认只核查，--apply才执行单事务迁移。

仅合并同名同行业、恰好一个已知股票代码主档和一个缺代码副档的组。
保留全部账号和密码，只迁移company_code；公司副档按明确主键逐条删除。
原始企业资料与引用ID保存到受保护目录，备份不读取或保存账号密码。
"""
import argparse
from collections import defaultdict, Counter
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import pymysql
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parent.parent
BACKUP = ROOT / ".runtime" / "company-identity"
REFERENCES = ("company_user", "company_data_missing_reminder", "company_data_change_notice")


def text(value):
    return "" if value is None else str(value).strip()


def kind(value):
    return text(value).replace("上市公司", "上市企业")


def plan_merge(rows):
    """只处理明确的缺代码副档；多个真实代码、资料冲突均交给人工核查。"""
    groups = defaultdict(list)
    for row in rows:
        groups[row["company_name"], row["industry_key"]].append(row)
    result, skipped = [], Counter()
    for group in groups.values():
        if len(group) < 2:
            continue
        known = [r for r in group if text(r["stock_code"]) not in ("", "-")]
        if len(group) != 2 or len(known) != 1:
            skipped["ambiguous_identity"] += 1
            continue
        main = known[0]
        duplicate = next(r for r in group if r is not main)
        if kind(main["company_type"]) != kind(duplicate["company_type"]):
            skipped["company_type_conflict"] += 1
            continue
        if any(text(main[f]) and text(duplicate[f]) and text(main[f]) != text(duplicate[f])
               for f in ("contact_name", "contact_phone", "address")):
            skipped["profile_conflict"] += 1
            continue
        result.append({"canonical": main, "duplicate": duplicate, "references": {}})
    return result, skipped


def run(apply=False, export_plan=False):
    env = {**dotenv_values(ROOT / "ml-service" / ".env.local"), **os.environ}
    db = pymysql.connect(host=env.get("DB_HOST", "127.0.0.1"), port=int(env.get("DB_PORT", 3306)),
            user=env.get("DB_USERNAME", "root"), password=env.get("DB_PASSWORD", ""),
            database=env.get("DB_NAME", "trans_finance"), charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor, autocommit=False)
    backup_path = None
    try:
        with db.cursor() as cur:
            # 有新增引用表时要求先扩展迁移，不忽略未知关联。
            cur.execute("""SELECT c.TABLE_NAME FROM information_schema.COLUMNS c
                JOIN information_schema.TABLES t ON t.TABLE_SCHEMA=c.TABLE_SCHEMA AND t.TABLE_NAME=c.TABLE_NAME
                WHERE c.TABLE_SCHEMA=DATABASE() AND c.COLUMN_NAME='company_code' AND t.TABLE_TYPE='BASE TABLE'""")
            actual = {r["TABLE_NAME"] for r in cur.fetchall()}
            if actual != {"company", *REFERENCES}:
                raise ValueError("Unexpected company references; manual review required")
            cur.execute("SELECT * FROM company" + (" FOR UPDATE" if apply else ""))
            plan, skipped = plan_merge(cur.fetchall())
            accepted = []
            counts = Counter()
            for entry in plan:
                old = entry["duplicate"]["company_code"]
                new = entry["canonical"]["company_code"]
                cur.execute("""SELECT COUNT(*) n FROM company_data_missing_reminder a
                    JOIN company_data_missing_reminder b ON a.source_table=b.source_table
                    AND a.source_row_id=b.source_row_id WHERE a.company_code=%s AND b.company_code=%s""", (old, new))
                if cur.fetchone()["n"]:
                    skipped["reminder_unique_conflict"] += 1
                    continue
                for table in REFERENCES:
                    cur.execute(f"SELECT id FROM `{table}` WHERE company_code=%s" + (" FOR UPDATE" if apply else ""), (old,))
                    entry["references"][table] = [r["id"] for r in cur.fetchall()]
                    counts[table] += len(entry["references"][table])
                accepted.append(entry)
            print(json.dumps({"mode": "apply" if apply else "audit", "merge_groups": len(accepted),
                              "reference_counts": dict(counts), "skipped": dict(skipped)}, ensure_ascii=True), flush=True)
            if export_plan:
                # 审阅清单只含企业身份与关联数量，不包含联系人、账号凭据或企业指标。
                summary = [{"company_name": e["canonical"]["company_name"],
                            "industry_key": e["canonical"]["industry_key"],
                            "keep_company_code": e["canonical"]["company_code"],
                            "remove_duplicate_code": e["duplicate"]["company_code"],
                            "stock_code": e["canonical"]["stock_code"],
                            "references": {t: len(ids) for t, ids in e["references"].items()}}
                           for e in accepted]
                with (ROOT / "database" / "company-identity-merge-plan.json").open("x", encoding="utf-8") as output:
                    json.dump(summary, output, ensure_ascii=False, indent=2)
            if not apply or not accepted:
                db.rollback()
                return
            if not BACKUP.is_dir():
                raise ValueError("Use repair-company-identity.ps1 to protect backup directory first")
            # 文件必须先成功落盘，再开始任何UPDATE/DELETE；唯一文件名避免覆盖恢复信息。
            stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
            backup_path = BACKUP / ("merge-" + stamp + ".json")
            backup = {"database": env.get("DB_NAME", "trans_finance"), "state": "prepared", "groups": accepted}
            with backup_path.open("x", encoding="utf-8") as output:
                json.dump(backup, output, ensure_ascii=False, indent=2, default=str)
                output.flush()
                os.fsync(output.fileno())
            for entry in accepted:
                old, new = entry["duplicate"]["company_code"], entry["canonical"]["company_code"]
                for field in ("contact_name", "contact_phone", "address"):
                    if not text(entry["canonical"][field]) and text(entry["duplicate"][field]):
                        cur.execute(f"UPDATE company SET `{field}`=%s WHERE company_code=%s", (entry["duplicate"][field], new))
                for table in REFERENCES:
                    for row_id in entry["references"][table]:
                        cur.execute(f"UPDATE `{table}` SET company_code=%s WHERE id=%s AND company_code=%s", (new, row_id, old))
                        if cur.rowcount != 1:
                            raise ValueError("Reference changed during migration")
                cur.execute("DELETE FROM company WHERE company_code=%s AND COALESCE(TRIM(stock_code),'') IN ('','-')", (old,))
                if cur.rowcount != 1:
                    raise ValueError("Company changed during migration")
            db.commit()
            print("Identity migration committed; account credentials and assessment rows unchanged.", flush=True)
            # 单独的完成标记，不覆盖原始备份；出现磁盘故障也保留完整恢复材料。
            try:
                backup_path.with_suffix(".committed").touch(exist_ok=False)
            except OSError:
                print("Migration committed; completion marker unavailable, original backup retained.", flush=True)
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--export-plan", action="store_true")
    args = parser.parse_args()
    try:
        run(args.apply, args.export_plan)
    except Exception as error:
        # 不输出可能含SQL参数、联系人或连接密码的底层异常。
        print("Identity migration stopped: " + type(error).__name__, flush=True)
        raise SystemExit(1) from None
