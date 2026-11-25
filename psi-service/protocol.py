"""MPC4J HFH99-ECC适配层，Windows上使用Java17，无需Docker或WSL。

每次求交创建独立Java进程；本行数据通过匿名管道进入引擎，不写入文件。
密码学实现全部由MPC4J提供，银行之间只传输加密数据包。
"""
import json
import os
import secrets
import subprocess
import threading
from pathlib import Path

MAX_ITEMS = 100000
MAX_WIRE = 24 * 1024 * 1024
ROOT = Path(__file__).resolve().parent
JAR = ROOT / "java" / "target" / "psi-engine.jar"


def available():
    return JAR.is_file()


def values_for_protocol(values):
    # MPC4J要求非空集合。空集用本次随机占位元素参与，结果不会映射到企业。
    result = list(dict.fromkeys(values))
    if len(result) > MAX_ITEMS:
        raise ValueError("PSI input too large")
    # 打乱身份、指标、歧义标记的排列，避免原始行顺序通过密文位置暴露。
    secrets.SystemRandom().shuffle(result)
    return result or ["psi-empty:" + secrets.token_hex(32)]


class Engine:
    """超时看门狗确保连接中断也不会永久留下携带本行数据的子进程。"""
    def __init__(self, initial):
        if not available():
            raise RuntimeError("Build the Java PSI engine first")
        java = os.environ.get("PSI_JAVA", "java")
        self.process = subprocess.Popen(
            [java, "-Xmx384m", "-ea", "-jar", str(JAR)],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
            text=True, encoding="utf-8",
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        self.timer = threading.Timer(90, self.process.kill)
        self.timer.daemon = True
        self.timer.start()
        try:
            self.send(initial)
        except Exception:
            self.close()
            raise

    def send(self, value):
        line = json.dumps(value, ensure_ascii=False, separators=(",", ":"))
        if len(line.encode("utf-8")) > MAX_WIRE:
            raise ValueError("PSI input too large")
        self.process.stdin.write(line + "\n")
        self.process.stdin.flush()

    def receive(self):
        line = self.process.stdout.readline(MAX_WIRE + 1)
        if not line or len(line) > MAX_WIRE:
            raise RuntimeError("PSI engine failed")
        value = json.loads(line)
        if not isinstance(value, dict):
            raise ValueError("Invalid engine response")
        return value

    def close(self):
        self.timer.cancel()
        if self.process.poll() is None:
            self.process.kill()
        self.process.wait(timeout=5)
        self.process.stdin.close()
        self.process.stdout.close()


def request(values, server_count):
    if not 0 < server_count <= MAX_ITEMS:
        raise ValueError("Invalid peer count")
    client = Engine({"role": "client", "values": values_for_protocol(values), "peerCount": server_count})
    try:
        encrypted = client.receive()["encrypted"]
        return client, json.dumps(encrypted, separators=(",", ":"))
    except Exception:
        client.close()
        raise


def respond(values, encrypted, client_count):
    if not 0 < client_count <= MAX_ITEMS or len(encrypted) > MAX_WIRE:
        raise ValueError("Invalid PSI input size")
    packet = json.loads(encrypted)
    if not isinstance(packet, list) or len(packet) != client_count:
        raise ValueError("Invalid client packet")
    server = Engine({"role": "server", "values": values_for_protocol(values),
                     "peerCount": client_count, "encrypted": packet})
    try:
        return server.receive()
    finally:
        server.close()


def finish(client, values, envelope):
    try:
        client.send(envelope)
        matches = client.receive()["matches"]
        if not isinstance(matches, list) or not set(matches).issubset(set(values)):
            raise ValueError("Invalid PSI result")
        return set(matches)
    finally:
        client.close()
