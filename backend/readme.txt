后端目录说明

1. 目录作用
本目录是《企业转型金融数据平台》的 Java 后端服务，负责：
- 金融机构端登录、注册、模型训练、联邦学习、企业评估数据管理
- 企业端登录、注册、待补全数据、历史数据、待评估数据、评估报告
- 与 MySQL 数据库交互
- 向 Python 模型服务发起训练与预测请求

2. 技术栈
- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Maven

3. 目录结构说明
- `pom.xml`
  - Maven 依赖与打包配置文件。
- `src/main/java/com/transfinance/TransFinanceApplication.java`
  - 后端启动入口。
- `src/main/java/com/transfinance/common/`
  - 通用返回对象、分页对象等基础封装。
- `src/main/java/com/transfinance/config/`
  - Spring Boot 配置类，例如 CORS、RestClient、MyBatis-Plus 等。
- `src/main/java/com/transfinance/controller/`
  - 所有 REST 接口控制器。
  - 这里负责接收前端请求、参数校验、返回统一响应。
- `src/main/java/com/transfinance/dto/`
  - 请求数据对象。
  - 用于登录、注册、分页查询、新增、修改、评分等接口入参。
- `src/main/java/com/transfinance/entity/`
  - 数据库实体类，对应 MySQL 表。
- `src/main/java/com/transfinance/exception/`
  - 业务异常和全局异常处理。
- `src/main/java/com/transfinance/mapper/`
  - MyBatis Mapper 接口，负责 SQL 查询与写入。
- `src/main/java/com/transfinance/service/`
  - 业务接口定义。
- `src/main/java/com/transfinance/service/impl/`
  - 业务实现层，真正承载登录、训练、评估、授信、归档等流程。
- `src/main/java/com/transfinance/support/`
  - 行业元数据、字段白名单、动态表结构等支撑类。
- `src/main/java/com/transfinance/util/`
  - 工具类，例如企业编码生成器。
- `src/main/java/com/transfinance/vo/`
  - 返回视图对象，供前端展示使用。
- `src/main/resources/application.yml`
  - 主配置文件，包含端口、数据库连接、模型服务地址等。
- `src/main/resources/application-local.yml`
  - 本地开发配置。
- `src/main/resources/application-doubao.yml`
  - AI 解读相关配置。
- `src/test/java/`
  - 测试代码。

4. 关键文件说明
- `controller/AuthController.java`
  - 金融机构登录注册接口。
- `controller/CompanyAuthController.java`
  - 企业登录注册、企业资料修改接口。
- `controller/CompanyDataController.java`
  - 企业端历史数据与待评估数据接口。
- `controller/ElectricCompanyDataController.java`
  - 金融机构端行业评估数据 CRUD 接口。
- `controller/PendingCompanyDataController.java`
  - 待评估数据、评分、授信、删除接口。
- `controller/ModelTrainingController.java`
  - 本机构模型训练、更新、清空、通知企业等接口。
- `controller/FederatedLearningController.java`
  - 联邦学习训练、清空、概览接口。
- `controller/AssessmentReportController.java`
  - 评估报告生成接口。
- `service/impl/CompanyDataServiceImpl.java`
  - 企业端历史/待评估数据查询核心逻辑。
- `service/impl/ElectricCompanyDataServiceImpl.java`
  - 金融机构端企业数据增删改查、评分、授信、归档核心逻辑。
- `service/impl/ModelTrainingServiceImpl.java`
  - 本地模型训练、清空模型、通知企业。
- `service/impl/FederatedLearningServiceImpl.java`
  - 联邦模型聚合与清空。
- `service/impl/AssessmentReportServiceImpl.java`
  - 报告字段、雷达图数据、权重与结论生成。
- `service/impl/AssessmentNarrativeServiceImpl.java`
  - AI 报告解读文本生成。

5. 启动方式
- 确认 MySQL 已启动并导入 `TransformationalFinance.sql`
- 在 `backend` 目录执行 `mvn spring-boot:run`
- 默认端口：`8080`

6. 交付注意事项
- `target/` 是构建产物，不需要手工修改。
- 不要把数据库密码、Token、API Key 写进源码。
- 如果修改数据库结构，优先同步更新 `database/` 和 `TransformationalFinance.sql`。
