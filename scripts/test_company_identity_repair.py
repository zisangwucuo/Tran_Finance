"""身份修复计划回归测试，不连接数据库。"""
import unittest
from repair_company_identity import plan_merge


def company(code, stock, **extra):
    return {"company_code": code, "company_name": "示例企业", "industry_key": "coal",
            "stock_code": stock, "company_type": "上市企业", "contact_name": None,
            "contact_phone": None, "address": None, **extra}


class IdentityRepairTests(unittest.TestCase):
    def test_select_known_stock_as_canonical(self):
        plan, skipped = plan_merge([company("old", "601898"), company("duplicate", "-")])
        self.assertEqual(plan[0]["canonical"]["company_code"], "old")
        self.assertFalse(skipped)

    def test_distinct_real_stocks_not_merged(self):
        plan, skipped = plan_merge([company("one", "601898"), company("two", "600985")])
        self.assertFalse(plan)
        self.assertEqual(skipped["ambiguous_identity"], 1)

    def test_conflicting_profile_not_lost(self):
        plan, skipped = plan_merge([company("one", "601898", address="地址甲"), company("two", "-", address="地址乙")])
        self.assertFalse(plan)
        self.assertEqual(skipped["profile_conflict"], 1)

    def test_different_industries_not_merged(self):
        plan, _ = plan_merge([company("one", "601898"), company("two", "-", industry_key="power")])
        self.assertFalse(plan)


if __name__ == "__main__":
    unittest.main()
