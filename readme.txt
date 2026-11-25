企业转型金融数据评估平台，点击run.bat，输入数据库端口及密码。可直接运行服务启动网页，但需确保电脑存在jdk17，python3.10，mysqlserver，以及运行过同文件夹下的sql文件。
一、项目说明
本项目是一个前后端分离的企业转型金融数据评估平台，包含：
1. 金融机构端
2. 企业端
3. Python 模型服务
4. MySQL 数据库
5. AI 报告解读能力（可选，接入豆包）

二、你需要安装的软件和服务
1. JDK 17 或更高版本
   用于运行后端 Spring Boot 服务。
   推荐下载：
   https://adoptium.net/temurin/releases/?package=jdk

2. Python 3.10 或更高版本
   用于运行模型服务。
   下载地址：
   https://www.python.org/downloads/windows/

3. MySQL Server 8
   用于保存业务数据。
   下载地址：
   https://dev.mysql.com/downloads/mysql/


三、当前默认运行配置
以下配置按照本机当前环境编写：

1. 数据库
   - 主机：127.0.0.1
   - 端口：3306
   - 数据库名：trans_finance
   - 用户名：root
   - 密码：<填写本机数据库密码>

2. 后端
   - 访问端口：8080
   - 项目路径：backend

3. 前端开发态
   - 访问端口：5173
   - 项目路径：frontend

4. 模型服务
   - 访问端口：8090
   - 项目路径：ml-service

四、数据库初始化
如果你是在一台全新的电脑上运行，先完成数据库初始化。

1. 启动 MySQL Server 8
2. 使用 MySQL Workbench 或命令行连接数据库
3. 执行根目录下的：
   TransformationalFinance.sql

说明：
- 现在主初始化脚本只需要运行这一份
- 一般不需要再手动执行 database/ 目录下的拆分脚本


五、后端启动方式
方式一：IDEA 启动（开发模式）

1. 用 IDEA 打开 backend 目录
2. 确认 JDK 版本是 17
3. 确认数据库已启动
4. 检查配置文件：
   - backend/src/main/resources/application.yml
   - backend/src/main/resources/application-local.yml
5. 请通过 DB_PASSWORD 环境变量或本地配置提供数据库密码：
   <填写本机数据库密码>
6. 启动主类：
   com.transfinance.TransFinanceApplication

方式二：命令行启动（开发模式）
在 backend 目录执行：

   mvn spring-boot:run

六、前端启动方式
方式一：VS Code 开发模式

1. 用 VS Code 打开 frontend 目录
2. 第一次运行先安装依赖：
   npm install
3. 启动前端：
   npm run dev
4. 默认访问：
   http://localhost:5173

方式二：发布模式
前端会先打包，再由后端 jar 托管。

七、模型服务启动方式
模型服务负责训练模型和进行评分预测。

1. 进入 ml-service 目录
2. 创建虚拟环境：
   python -m venv .venv
3. 安装依赖：
   .\.venv\Scripts\python.exe -m pip install -r requirements.txt
4. 配置数据库环境变量
   你可以直接创建 ml-service/.env.local 文件，内容参考 README.md 的数据库配置说明
   当前本机建议写成：
   DB_HOST=127.0.0.1
   DB_PORT=3306
   DB_NAME=trans_finance
   DB_USERNAME=root
   DB_PASSWORD=<填写本机数据库密码>
5. 启动模型服务：
   .\scripts\start-ml-service.ps1

如果你想手动启动，也可以直接执行：

   .\.venv\Scripts\uvicorn.exe app:app --host 127.0.0.1 --port 8090

八、AI 报告解读配置（豆包）
如果你要让系统在生成报告时使用 AI 文本解读，需要启用后端的 AI 配置。

1. 打开后端的配置文件：
   backend/src/main/resources/application-doubao.yml

2. 推荐使用的环境变量：
   - SPRING_PROFILES_ACTIVE=doubao
   - AI_NARRATIVE_ENABLED=true
   - ARK_API_KEY=你的豆包 API Key
   - ARK_MODEL=你的模型名
   - AI_NARRATIVE_API_URL=https://ark.cn-beijing.volces.com/api/v3/chat/completions

3. 如果你不使用 AI：
   - 不必设置以上变量
   - 保持 AI_NARRATIVE_ENABLED=false 即可

说明：
- application.yml 默认关闭 AI 文本解读
- application-doubao.yml 会在启用 doubao profile 时使用
- 不要把真实 API Key 写进源码仓库

九、推荐运行顺序
如果想在一台新电脑上完整跑起来，按这个顺序：

1. 安装 MySQL Server 8
2. 安装 JDK 17
3. 安装 Python 3.10+
4. 安装 Node.js LTS
5. 安装 Maven（如果要重新构建）
6. 导入 TransformationalFinance.sql
7. 启动模型服务
8. 启动后端
9. 启动前端开发服务器，或者使用发布模式

十、发布模式一键运行
如果你只是想让别人解压后直接跑，可以使用发布模式。

最推荐方式：

1. 确认 MySQL Server 已启动
2. 确认已经导入 TransformationalFinance.sql
3. 双击根目录下的：
   run.bat
4. 根据提示输入数据库主机、端口、用户名、密码
5. 如果需要豆包 AI 报告解读，根据提示启用并输入模型名、API Key
6. 脚本会自动启动模型服务、后端 jar，并打开浏览器

说明：
- run.bat 不会把数据库密码和豆包 API Key 写入项目文件
- 数据库密码和 AI Key 只会作为本次启动的运行时环境变量传给后端和模型服务
- 如果没有导入数据库，网页可以打开，但不会有业务数据
- 如果没有配置豆包 AI，系统仍可运行，只是报告解读不会调用真实 AI 服务

如果需要重新构建发布包：

1. 先构建：
   .\scripts\build-packaged-app.ps1
2. 再启动：
   run.bat
3. 打开浏览器访问：
   http://localhost:8080

说明：
- 发布模式下，前端静态文件已经打进后端 jar
- 仍然需要 MySQL 正常运行

十一、常见问题
1. 打开页面提示 Network Error
   请检查：
   - 后端是否启动
   - MySQL 是否启动
   - 模型服务是否启动
   - 浏览器访问地址是否正确

2. 登录后页面空白
   先确认后端和前端是否都启动成功。
   发布模式下请直接访问 http://localhost:8080

3. AI 解读没有内容
   请检查：
   - 是否启用了 doubao profile
   - API Key 是否正确
   - 模型名是否正确
   - 网络是否能访问豆包接口

4. 模型训练失败
   请检查：
   - ml-service 是否启动
   - 数据库连接是否正确
   - 模型服务依赖是否安装完整

十二、项目内主要目录作用
1. backend
   Java 后端服务，提供登录、注册、数据管理、训练管理、报表等接口。

2. frontend
   Vue 前端页面，提供金融机构端和企业端的界面。

3. ml-service
   Python 模型服务，负责训练、预测、联邦学习和评分接口。

4. database
   历史拆分 SQL 和辅助脚本。

5. scripts
   一键启动、打包、数据处理脚本。

6. TransformationalFinance.sql
   主数据库初始化脚本。

十三、8080 端口占用处理
run.bat 在启动后端 jar 之前，会自动检查本机是否有程序正在监听 8080 端口。
如果发现 8080 已被占用，脚本会结束对应进程，然后再启动本系统后端。

注意：
- 脚本只处理 LISTENING 状态的本机 8080 端口
- 普通网络连接中出现的远程 8080 不会被处理
- 如果系统提示权限不足，请右键 run.bat，选择“以管理员身份运行”
