"""单机构PSI节点。每个Windows进程使用自己的配置和本方只读数据库账号。

管理员入口供Java网关发起任务；机构间入口仅收PSI密文，二者凭据互不通用。
HTTP仅监听127.0.0.1；多机部署须另加TLS及机构级身份认证。
"""
import json
import os
import secrets
import threading
import time
import urllib.request
from pathlib import Path

import pymysql
from fastapi import FastAPI, Header, HTTPException, Depends
from pydantic import BaseModel, Field
from records import prepare, inputs, summarize
from protocol import request, respond, finish, available

CONFIG = json.loads(Path(os.environ.get("PSI_CONFIG", "/config/node.json")).read_text(encoding="utf-8"))
app = FastAPI(title="Bank PSI node", docs_url=None, redoc_url=None, openapi_url=None)
lock = threading.Lock()
last_run = 0.0
exchange_slots = threading.BoundedSemaphore(2)
PROTOCOL = "MPC4J-1.1.5-HFH99-ECC-FIELDS-v2"


def schema(scope):
    """仅交换公开字段定义，避免不同节点版本或字段配置造成假差异。"""
    return CONFIG["industries"][scope.industry]["fields"]


class Scope(BaseModel):
    industry: str
    dataset: str


class Exchange(Scope):
    protocol: str
    fields: list[dict]
    encrypted: str = Field(max_length=24 * 1024 * 1024)
    count: int = Field(gt=0, le=100000)
    server_count: int = Field(gt=0, le=100000)


def admin(authorization: str = Header(default="")):
    if not secrets.compare_digest(authorization, "Bearer " + CONFIG["adminToken"]):
        raise HTTPException(401, "Node authentication required")


def peer(x_peer: str = Header(default=""), authorization: str = Header(default="")):
    entry = CONFIG["peers"].get(x_peer)
    if not entry or not secrets.compare_digest(authorization, "Bearer " + entry["token"]):
        raise HTTPException(401, "Peer authentication required")


def read_local(scope):
    """白名单选择本节点视图，用户不能传入表名、查询条件或任意匹配候选值。"""
    meta = CONFIG["industries"].get(scope.industry)
    if not meta or scope.dataset not in ("history", "pending"):
        raise HTTPException(400, "Invalid scope")
    table = meta[scope.dataset]
    with pymysql.connect(**CONFIG["database"], charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor) as conn:
        with conn.cursor() as cursor:
            cursor.execute(f"SELECT * FROM `{table}`")
            rows = cursor.fetchall()
    return prepare(rows, scope.industry, scope.dataset, meta["fields"])


@app.get("/health", dependencies=[Depends(admin)])
def health():
    try:
        if not available():
            raise RuntimeError("PSI engine missing")
        with pymysql.connect(**CONFIG["database"], connect_timeout=3) as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT 1")
        return {"institutionId": CONFIG["institutionId"], "status": "READY", "protocol": PROTOCOL}
    except Exception:
        raise HTTPException(503, "Node database unavailable") from None


@app.post("/count", dependencies=[Depends(peer)])
def count(scope: Scope):
    # 协议要求双方知道集合规模，仅返回数量，不提供候选身份查询入口。
    try:
        groups, _ = read_local(scope)
        return {"count": max(1, len(inputs(groups, receiver=False))), "protocol": PROTOCOL, "fields": schema(scope)}
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(503, "Node data unavailable") from None


@app.post("/exchange", dependencies=[Depends(peer)])
def exchange(scope: Exchange):
    if not exchange_slots.acquire(blocking=False):
        raise HTTPException(429, "Node busy")
    try:
        groups, _ = read_local(scope)
        if scope.protocol != PROTOCOL or scope.fields != schema(scope):
            raise HTTPException(409, "Node protocol or schema mismatch; restart all nodes")
        values = inputs(groups, receiver=False)
        if max(1, len(values)) != scope.server_count:
            raise HTTPException(409, "Dataset changed; retry")
        return respond(values, scope.encrypted, scope.count)
    except HTTPException:
        raise
    except Exception:
        # 不将库异常、SQL或输入内容写入日志和响应。
        raise HTTPException(503, "PSI exchange failed") from None
    finally:
        exchange_slots.release()


def peer_call(node, path, body):
    req = urllib.request.Request(node["url"] + path, data=json.dumps(body).encode(),
            headers={"Content-Type": "application/json", "X-Peer": CONFIG["institutionId"],
                     "Authorization": "Bearer " + node["token"]})
    # 本机节点直连，不继承外部代理；响应长度受限，避免异常节点耗尽内存。
    with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(req, timeout=90) as response:
        raw = response.read(24 * 1024 * 1024 + 1)
        if len(raw) > 24 * 1024 * 1024:
            raise ValueError("Peer response too large")
        return json.loads(raw)


@app.post("/run", dependencies=[Depends(admin)])
def run(scope: Scope):
    global last_run
    if not lock.acquire(blocking=False):
        raise HTTPException(409, "A check is already running")
    try:
        if time.monotonic() - last_run < 3:
            raise HTTPException(429, "Please wait before checking again")
        last_run = time.monotonic()
        groups, skipped = read_local(scope)
        local_inputs = inputs(groups, receiver=True)
        result = []
        if local_inputs:
            for code, node in CONFIG["peers"].items():
                body = {"industry": scope.industry, "dataset": scope.dataset}
                metadata = peer_call(node, "/count", body)
                if metadata.get("protocol") != PROTOCOL or metadata.get("fields") != schema(scope):
                    raise HTTPException(409, "Node protocol or schema mismatch; restart all nodes")
                server_count = metadata["count"]
                client, encrypted = request(local_inputs, server_count)
                try:
                    envelope = peer_call(node, "/exchange", {**body, "encrypted": encrypted,
                                          "count": len(local_inputs), "server_count": server_count,
                                          "protocol": PROTOCOL, "fields": schema(scope)})
                    result.extend(summarize(groups, finish(client, local_inputs, envelope), code, node["name"]))
                finally:
                    client.close()
        return {"records": result, "skippedLocalRows": skipped,
                "localCustomerYears": len(groups), "checkedPeers": list(CONFIG["peers"]) if local_inputs else []}
    except HTTPException:
        raise
    except Exception:
        # 任一节点失败则整次检查失败，不把部分结果误报为不存在共同客户。
        raise HTTPException(503, "PSI check failed; verify all bank nodes") from None
    finally:
        lock.release()
