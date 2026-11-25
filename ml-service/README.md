# 转型金融模型服务

该服务负责为每家金融机构按八个行业分别训练八个随机森林模型。
模型目标变量只有“经核验转型绩效”，输入为通用指标与各行业专项指标，预测概率乘以 100
后作为转型综合评分；训练完成后同时保存特征权重，供后续指标贡献度分析使用。

联邦学习版块复用这些本地随机森林模型：同一行业下，各金融机构只共享模型结构、模型指标和特征重要性，不共享企业原始数据；联邦模型按各机构训练样本数加权集成预测概率。

## 启动

请先设置数据库连接环境变量，不要把真实密码写入代码：

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="trans_finance"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8090
```

模型文件默认保存到 `ml-service/models/`，该目录不应提交到代码库。
