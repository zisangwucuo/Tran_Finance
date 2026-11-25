"""独立于密码学引擎的数据规范化测试；真实协议另由test_protocol.py验证。"""
import unittest
from records import prepare, inputs, summarize, normalized


class RecordTests(unittest.TestCase):
    def groups(self, value, year=2025):
        return prepare([{"psi_company_code": "demo", "company_name": "示例企业",
                         "data_year": year, "indicator": value}], "power", "history",
                       [{"column": "indicator", "type": "number"}])[0]

    def compare(self, a, b):
        # 这里只模拟集合交集来验证三态判定，不作为生产PSI实现。
        matches = set(inputs(a, True)) & set(inputs(b, False))
        return summarize(a, matches, "bank_b", "示例银行")

    def test_numeric_format(self):
        self.assertEqual(self.compare(self.groups("1.00"), self.groups(1))[0]["status"], "CONSISTENT")

    def test_missing_is_not_zero(self):
        self.assertEqual(self.compare(self.groups(None), self.groups(0))[0]["status"], "DIFFERENT")
        self.assertEqual(self.compare(self.groups(None), self.groups(0))[0]["differentFields"], ["indicator"])
        self.assertEqual(normalized(" ", "number"), None)

    def test_other_year_not_common(self):
        self.assertEqual(self.compare(self.groups(1), self.groups(1, 2024)), [])

    def test_conflicting_duplicate(self):
        other = self.groups(1)
        next(iter(other.values()))["payloads"].add("conflicting record")
        self.assertEqual(self.compare(self.groups(1), other)[0]["status"], "AMBIGUOUS")

    def test_no_peer_payload_in_output(self):
        record = self.compare(self.groups(1), self.groups(2))[0]
        self.assertEqual(set(record), {"companyCode", "companyName", "year", "otherInstitution", "otherInstitutionName", "status", "differentFields"})

    def test_only_changed_fields(self):
        fields = [{"column": "numeric", "type": "number"}, {"column": "flag", "type": "boolean"},
                  {"column": "rating", "type": "text"}]
        base = {"psi_company_code": "demo", "company_name": "示例", "data_year": 2025}
        a = prepare([{**base, "numeric": "1.00", "flag": True, "rating": "A"}], "power", "pending", fields)[0]
        b = prepare([{**base, "numeric": 1, "flag": False, "rating": "B"}], "power", "pending", fields)[0]
        self.assertEqual(self.compare(a, b)[0]["differentFields"], ["flag", "rating"])
        self.assertEqual(self.compare(a, a)[0]["differentFields"], [])


if __name__ == "__main__":
    unittest.main()
