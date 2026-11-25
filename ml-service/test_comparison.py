"""对比协议回归：相同样本、严格时间外划分、缺失处理、不可比较数据拒绝。"""
import unittest
import tempfile
import time
from pathlib import Path
from unittest.mock import patch
from comparison import evaluate
from app import normalize_value


class ComparisonTest(unittest.TestCase):
    def rows(self):
        return [dict(company_name=f"企业{i}", data_year=year, verified_transition_performance=i % 2,
                     feature=None if i % 5 == 0 else i / 20)
                for year in [2020, 2021, 2022] for i in range(20)]

    def evaluate(self, rows):
        return evaluate(rows, [("feature", "feature", "score")], normalize_value)

    def test_shared_temporal_split_and_probabilities(self):
        rows = self.evaluate(self.rows())
        self.assertEqual(len(rows), 4)
        self.assertEqual(len({r["dataFingerprint"] for r in rows}), 1)
        for row in rows:
            self.assertEqual(row["trainYears"], [2020, 2021])
            self.assertEqual(row["testYear"], 2022)
            self.assertEqual(row["trainSamples"], 40)
            self.assertEqual(row["testSamples"], 20)
            self.assertEqual(row["missingTrainValues"], 8)
            self.assertTrue(0 <= row["auc"] <= 1)
            self.assertTrue(0 <= row["brier"] <= 1)

    def test_degenerate_training_and_duplicates_rejected(self):
        rows = self.rows()
        with self.assertRaisesRegex(ValueError, "重复"):
            self.evaluate(rows + [rows[0]])
        for row in rows:
            row["verified_transition_performance"] = 1
        with self.assertRaisesRegex(ValueError, "一个类别"):
            self.evaluate(rows)

    def test_single_class_test_not_fake_auc(self):
        rows = self.rows()
        for row in rows:
            if row["data_year"] == 2022:
                row["verified_transition_performance"] = 1
        for result in self.evaluate(rows):
            self.assertIsNone(result["auc"])
            self.assertIsNone(result["ap"])

    def test_imputer_fits_training_only(self):
        import comparison
        original = comparison.SimpleImputer.fit
        sizes = []
        def fit(instance, x, y=None):
            sizes.append(len(x))
            return original(instance, x, y)
        with patch.object(comparison.SimpleImputer, "fit", fit):
            self.evaluate(self.rows())
        self.assertEqual(sizes, [20, 40, 20, 40, 20, 40, 20, 40])

    def test_async_results_persist_and_are_bank_scoped(self):
        import comparison
        with tempfile.TemporaryDirectory() as temporary, patch.object(comparison, "ROOT", Path(temporary)):
            result = comparison.start("a", "power", {"power": ("电力", "unused")},
                                      lambda bank, table: self.rows(),
                                      lambda key: [("feature", "feature", "score")], normalize_value)
            deadline = time.monotonic() + 15
            while "a" in comparison.ACTIVE and time.monotonic() < deadline:
                time.sleep(.05)
            saved = comparison.history("a")
            self.assertEqual(saved[0]["id"], result["id"])
            self.assertEqual(saved[0]["status"], "SUCCESS")
            self.assertEqual(len(saved[0]["results"]), 4)
            self.assertEqual(comparison.history("b"), [])

    def test_tuning_budget_and_test_labels_do_not_select_parameters(self):
        first = evaluate(self.rows(), [("feature", "feature", "score")], normalize_value, "tuned")
        changed = self.rows()
        for row in changed:
            if row["data_year"] == 2022:
                row["verified_transition_performance"] = 1 - row["verified_transition_performance"]
                row["feature"] = 999
        second = evaluate(changed, [("feature", "feature", "score")], normalize_value, "tuned")
        for left, right in zip(first, second):
            self.assertEqual(left["parameters"], right["parameters"])
            self.assertEqual(left["tuning"], right["tuning"])
            self.assertEqual(left["thresholdSelection"], right["thresholdSelection"])
            self.assertEqual(left["tuning"]["candidateCount"], 8)
            self.assertEqual(left["tuning"]["validationYears"], [2021])

    def test_no_temporal_validation_does_not_silently_use_test_year(self):
        with self.assertRaisesRegex(ValueError, "内部|训练年份内"):
            evaluate([r for r in self.rows() if r["data_year"] != 2020],
                     [("feature", "feature", "score")], normalize_value, "tuned")

    def test_raw_values_preserve_out_of_range_information(self):
        import pandas as pd
        from comparison import feature_frame
        frame = pd.DataFrame({"f": [120., 180., None, float('inf')]})
        definitions = [("f", "f", "score")]
        raw = feature_frame(frame, definitions, normalize_value, True)
        self.assertEqual(raw.f.iloc[:2].tolist(), [120., 180.])
        self.assertTrue(raw.f.iloc[2:].isna().all())

    def test_no_predicted_positive_is_undefined_precision(self):
        from comparison import classification_result
        result = classification_result([0, 0, 1], [.1, .2, .4], .5)
        self.assertIsNone(result["precision"])
        self.assertEqual(result["recall"], 0)
        self.assertEqual(result["predictedPositive"], 0)
        self.assertEqual(result["confusionMatrix"], [[2, 0], [1, 0]])

    def test_default_without_inner_fold_preserves_fixed_result(self):
        rows = evaluate([r for r in self.rows() if r["data_year"] != 2020],
                        [("feature", "feature", "score")], normalize_value)
        for row in rows:
            self.assertFalse(row["thresholdSelection"]["available"])
            self.assertIsNone(row["selectedThreshold"])
            self.assertEqual(row["fixedThreshold"]["threshold"], .5)

    def test_optimized_search_budget_and_test_isolation(self):
        first = evaluate(self.rows(), [("feature", "feature", "score")], normalize_value, "optimized")
        changed = self.rows()
        for row in changed:
            if row["data_year"] == 2022:
                row["verified_transition_performance"] = 1 - row["verified_transition_performance"]
                row["feature"] = 999
        second = evaluate(changed, [("feature", "feature", "score")], normalize_value, "optimized")
        for left, right in zip(first, second):
            self.assertEqual(left["parameters"], right["parameters"])
            self.assertEqual(left["tuning"], right["tuning"])
            self.assertEqual(left["tuning"]["candidateCount"], 32)
            self.assertEqual(left["featureMode"], "raw_with_missing_indicators")


if __name__ == "__main__":
    unittest.main()
