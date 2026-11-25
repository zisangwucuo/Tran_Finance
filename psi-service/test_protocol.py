"""直接调用Java协议库的互操作测试；不以普通集合交集代替被测实现。"""
import unittest
from protocol import request, respond, finish


class ProtocolTests(unittest.TestCase):
    def test_encrypted_field_differences(self):
        from records import prepare, inputs, summarize
        base = {"psi_company_code": "demo", "company_name": "示例", "data_year": 2025, "same": 1}
        fields = [{"column": key, "type": "number"} for key in ("same", "changed")]
        a = prepare([{**base, "changed": None}], "power", "pending", fields)[0]
        b = prepare([{**base, "changed": 42}], "power", "pending", fields)[0]
        matches = self.intersection(inputs(a, True), inputs(b, False))
        self.assertEqual(summarize(a, matches, "bank_b", "银行B")[0]["differentFields"], ["changed"])

    def intersection(self, a, b):
        client, encrypted = request(a, max(1, len(set(b))))
        try:
            response = respond(b, encrypted, max(1, len(set(a))))
            return finish(client, a, response)
        finally:
            client.close()

    def test_exact_common_values(self):
        self.assertEqual(self.intersection(["企业甲:2025", "企业乙:2024"],
                                          ["企业甲:2025", "企业乙:2025"]), {"企业甲:2025"})

    def test_empty_server(self):
        self.assertEqual(self.intersection(["企业甲"], []), set())

    def test_no_overlap(self):
        self.assertEqual(self.intersection(["a"], ["b"]), set())

    def test_ciphertext_changes_and_hides_plain_input(self):
        one, wire_one = request(["private-company-year"], 1)
        two, wire_two = request(["private-company-year"], 1)
        try:
            self.assertNotEqual(wire_one, wire_two)
            self.assertNotIn("private-company-year", wire_one)
        finally:
            one.close()
            two.close()

    def test_malformed_ciphertext_rejected(self):
        with self.assertRaises((ValueError, RuntimeError)):
            respond(["a"], '["invalid-ciphertext"]', 1)


if __name__ == "__main__":
    unittest.main()
