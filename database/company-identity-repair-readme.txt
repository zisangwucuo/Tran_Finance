企业身份修复说明

问题：历史数据带股票代码，待评估数据缺少股票代码，旧初始化逻辑重复创建企业。
新逻辑限定同名同行业，缺代码时复用唯一企业；多个候选或真实代码冲突时拒绝自动建档。
Java初始化、缺失提醒建档和Python初始化脚本已使用同样原则。
企业注册不再允许同名同行业重复建档，也不允许借注册直接认领原有企业账号。

迁移审阅清单：company-identity-merge-plan.json。
清单为预演结果，不表示数据库已修改，执行前会重新核查。
本次预演：152组，每组保留有股票代码的主档，移除缺代码的副档。
152个已有账号只更改company_code关联，用户名、密码和账号ID保留。
历史与待评估表的记录及股票代码不变；PSI视图随企业主表更新自动生效。
账号关联改变后企业用户需退出重新登录，旧会话/浏览器缓存可能仍保存旧企业编码。

根目录只读预演：
  .\ml-service\.venv\Scripts\python.exe scripts\repair_company_identity.py --export-plan
批准具体迁移范围后执行：
  powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\repair-company-identity.ps1 -Apply
执行会先创建受保护备份，再以单事务更新引用，最后按明确主键逐条删除副档。
冲突组跳过，未知引用表会停止迁移，失败事务回滚。
备份保存在.runtime/company-identity，仅当前Windows用户和SYSTEM可访问，已加入Git忽略。
备份包含原企业资料和被迁移引用的ID，不读取或保存密码。
数据库提交成功后生成同名.committed标记；若标记缺失，请核查数据库实际状态。
需要恢复时应依据备份核对当前引用，再以事务恢复，不要覆盖迁移后的新业务操作。

本迁移不自动改写TransformationalFinance.sql中的旧初始化快照。
其他电脑若导入包含重复企业的旧快照，先运行只读预演，再按审阅结果执行此修复。
