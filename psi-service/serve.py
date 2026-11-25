"""Windows单银行节点入口。命令行只传银行标识，不传密码或企业数据。"""
import os
import sys
from pathlib import Path
import uvicorn

if __name__ == "__main__":
    banks = ("bank_a", "bank_b", "bank_c")
    if len(sys.argv) != 2 or sys.argv[1] not in banks:
        raise SystemExit("Usage: serve.py bank_a|bank_b|bank_c")
    bank = sys.argv[1]
    config = Path(__file__).resolve().parent.parent / ".runtime" / "psi" / (bank + ".json")
    if not config.is_file():
        raise SystemExit("Prepare PSI node configuration first")
    os.environ["PSI_CONFIG"] = str(config)
    uvicorn.run("node:app", host="127.0.0.1", port=8111 + banks.index(bank),
                access_log=False, log_level="warning", limit_concurrency=16)
