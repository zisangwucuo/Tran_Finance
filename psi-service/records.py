"""规范化本机构记录。身份、指标和重复记录处理统一，避免格式差异误报。

所有字符串只在机构节点内形成；输出给协议库后，不以明文或普通摘要传输。
模型评分和授信标签不属于企业原始指标，不参与一致性判定。
"""
import json
import unicodedata
from decimal import Decimal, InvalidOperation


def normalized(value, kind):
    if value is None or isinstance(value, str) and not value.strip():
        return None
    if kind in ("number", "boolean"):
        if isinstance(value, bool):
            value = int(value)
        try:
            number = Decimal(str(value).strip())
            if not number.is_finite():
                raise ValueError("Non-finite indicator")
            return "0" if number == 0 else format(number.normalize(), "f")
        except InvalidOperation:
            raise ValueError("Invalid numeric indicator") from None
    return unicodedata.normalize("NFC", str(value).strip())


def encode(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def prepare(rows, industry, dataset, fields):
    """按统一企业编码和年度分组。无法唯一映射企业的记录跳过并报告数量。

    同一企业年度重复记录数值相同可合并；数值不同则标记待核查，不能任选一条。
    """
    groups = {}
    skipped = 0
    for row in rows:
        code = row.get("psi_company_code")
        year = row.get("data_year")
        if not code or year is None:
            skipped += 1
            continue
        identity = encode(["tf-psi-v1", industry, dataset, str(code), int(year)])
        payload = encode([[f["column"], normalized(row.get(f["column"]), f["type"])] for f in fields])
        group = groups.setdefault(identity, {
            "companyCode": str(code), "companyName": row["company_name"], "year": int(year),
            "payloads": set(),
        })
        group["payloads"].add(payload)
    return groups, skipped


def inputs(groups, receiver):
    """用域分隔同时做身份求交、整行一致性求交、重复歧义标识求交。

    接收方只提交从自身数据库构造的输入，不接受浏览器提供候选身份或指标。
    使用HFH99-ECC的等值核验，不使用Bloom过滤器；存在协议哈希的可忽略碰撞概率。
    """
    result = []
    for key, group in groups.items():
        result.append(encode(["identity", key]))
        if len(group["payloads"]) == 1:
            payload = next(iter(group["payloads"]))
            result.append(encode(["record", key, payload]))
            # 每个字段独立域分隔；只有本方已有值参与核验，不探测对方候选值。
            for column, value in json.loads(payload):
                result.append(encode(["field-v2", key, column, value]))
        if receiver or len(group["payloads"]) != 1:
            result.append(encode(["ambiguous", key]))
    return result


def summarize(groups, matches, peer, peer_name):
    """返回本方身份、三态结果及差异字段名称，绝不带对方原始值或摘要。"""
    result = []
    for key, group in groups.items():
        if encode(["identity", key]) not in matches:
            continue
        different_fields = []
        if len(group["payloads"]) != 1 or encode(["ambiguous", key]) in matches:
            status = "AMBIGUOUS"
        else:
            record = encode(["record", key, next(iter(group["payloads"]))])
            status = "CONSISTENT" if record in matches else "DIFFERENT"
            if status == "DIFFERENT":
                different_fields = [column for column, value in json.loads(next(iter(group["payloads"])))
                                    if encode(["field-v2", key, column, value]) not in matches]
        result.append({"companyCode": group["companyCode"], "companyName": group["companyName"],
                       "year": group["year"], "otherInstitution": peer,
                       "otherInstitutionName": peer_name, "status": status,
                       "differentFields": different_fields})
    return result
