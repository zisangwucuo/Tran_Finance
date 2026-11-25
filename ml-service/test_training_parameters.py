"""使用内存样本验证超参数实际进入森林，不连接数据库或覆盖业务模型。"""
import unittest
from unittest.mock import patch, MagicMock

import pandas as pd
from fastapi import BackgroundTasks
from pydantic import ValidationError
from app import ModelParameters, TrainRequest, fit_classifier, train_all


class TrainingParametersTest(unittest.TestCase):
    def test_defaults_and_custom_fit(self):
        x = pd.DataFrame({"a": list(range(40)), "b": [0, 1] * 20})
        y = pd.Series([0, 1] * 20)
        for parameters in [ModelParameters(), ModelParameters(nEstimators=50, maxDepth=3,
                minSamplesLeaf=3, minSamplesSplit=6, maxFeatures="all", classWeight="none", randomState=7)]:
            model, metrics = fit_classifier(x, y, parameters)
            self.assertEqual(len(model.estimators_), parameters.nEstimators)
            for key, value in parameters.classifier_options().items():
                self.assertEqual(model.get_params()[key], value)
            self.assertTrue(all(tree.tree_.max_depth <= parameters.maxDepth for tree in model.estimators_))
            self.assertEqual(metrics["validationMode"], "holdout")

    def test_invalid_parameters(self):
        for values in [{"nEstimators": 0}, {"maxDepth": 31}, {"minSamplesLeaf": None},
                       {"minSamplesSplit": 1}, {"randomState": -1}, {"nEstimators": 50.5},
                       {"classWeight": "invalid"}, {"maxFeatures": "invalid"}]:
            with self.subTest(values=values), self.assertRaises(ValidationError):
                ModelParameters(**values)

    @patch("app.connection")
    def test_task_parameters_and_baseline(self, connection):
        # 直接验证后台任务收到的快照：一键训练忽略自定义配置，更新采用自定义配置。
        connection.return_value.__enter__.return_value.cursor.return_value.__enter__.return_value.lastrowid = 1
        for update in [False, True]:
            tasks = BackgroundTasks()
            train_all(TrainRequest(institutionId="test", updateMode=update,
                                   parameters=ModelParameters(nEstimators=50)), tasks)
            self.assertEqual(tasks.tasks[0].args[2].nEstimators, 50 if update else 300)


if __name__ == "__main__":
    unittest.main()
