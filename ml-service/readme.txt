模型服务目录说明

1. 目录作用
本目录是 Python 模型服务，负责：
- 本机构行业模型训练
- 模型更新
- 联邦随机森林聚合训练
- 单条数据评分预测
- 返回转型贷款概率与评分

2. 技术栈
- Python 3.10+
- FastAPI
- Uvicorn
- scikit-learn
- pandas
- joblib
- PyMySQL

3. 目录结构说明
- `app.py`
  - 模型服务主程序。
  - 包含健康检查、训练接口、预测接口、联邦训练、联邦预测、模型文件保存和数据库元数据写入。
- `requirements.txt`
  - Python 依赖清单。
- `.env.example`
  - 数据库环境变量示例。
- `models/`
  - 模型文件输出目录。
  - 训练后生成的 `.joblib` 模型会放在这里。
- `README.md`
  - 原始英文说明文档，可保留参考。

4. `app.py` 里的核心职责
- `/health`
  - 模型服务健康检查与版本检查。
- `/ml/train-all`
  - 为当前金融机构提交八个行业的本地模型训练任务。
- `/ml/predict`
  - 使用本机构行业模型预测转型贷款概率。
- `/ml/federated/train`
  - 按行业聚合多个金融机构的本地随机森林模型。
- `/ml/federated/predict`
  - 使用联邦模型输出行业级评分概率。
- `train_single_model`
  - 单行业单机构训练。
- `fit_classifier`
  - 随机森林训练与简单兜底分类器。
- `build_feature_frame`
  - 特征归一化与矩阵构造。
- `aggregate_federated_industry_model`
  - 联邦权重聚合逻辑。
- `save_model_metadata`
  - 本地模型元数据入库。
- `save_federated_model_metadata`
  - 联邦模型元数据入库。

5. 环境变量说明
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

这些变量定义在 `.env.example` 中，实际运行时需要改成真实值。

6. 启动方式
- 先配置数据库连接环境变量
- 安装依赖：`pip install -r requirements.txt`
- 启动服务：`uvicorn app:app --host 0.0.0.0 --port 8090`

7. 交付注意事项
- `models/` 下的模型文件是运行时产物，不建议手工修改。
- 如果后端 `application.yml` 里的 `ml.service-url` 变了，这里也要同步。
- 模型服务不保存业务密码，不要把数据库密码写死在代码里。
