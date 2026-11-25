三银行隐私求交（Windows原生版）

一、当前方案
本模块使用MPC4J 1.1.5的HFH99-ECC协议，强制STANDARD_JDK环境。
底层为Bouncy Castle P-256纯Java实现，无需Docker、WSL、Ubuntu、C++编译器。
Java处理真实加密求交，Python负责本行数据读取、HTTP鉴权和结果映射。
每个银行是独立Windows进程，各自持有只读数据库账号。
本机已验证真实协议、三银行节点、48张视图隔离，以及三银行八行业历史/待评估共48种范围的求交结果。

二、启动与停止
在项目根目录双击start-psi.bat。停止时双击stop-psi.bat。
节点只监听127.0.0.1:8111、8112、8113；不会占用后端8080或模型服务端口。
保留现有后端、前端、模型服务的启动方式。PSI不会训练或删除模型。
后端重启后需要重新登录，再打开“联邦学习 / 查找共同客户”。
点击刷新节点状态，三个节点就绪后选择行业、历史或待评估数据，点击开始隐私求交。

首次准备需要：
1. Java 17（运行引擎），Python 3.11或更高版本（本机使用项目现有.venv）。
2. 本地MySQL，DB配置来自ml-service/.env.local或环境变量。
3. 在根目录安装Python依赖：
   .\ml-service\.venv\Scripts\python.exe -m pip install -r psi-service\requirements.txt
4. 如果没有psi-service/java/target/psi-engine.jar，启动脚本会调用Maven构建。
   构建需要JDK17、Maven和网络；拿到已构建JAR的人不需要Maven。
   独立构建：
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-psi-engine.ps1

不要通过运行整个TransformationalFinance.sql初始化本模块。
初始化只新增tf_psi_bank_a、tf_psi_bank_b、tf_psi_bank_c视图库和随机只读账号。
账号仅允许localhost来源，不能修改业务表，也不能读其他银行的视图。
三个银行必须已存在于financial_institution，编码为bank_a、bank_b、bank_c。
当前节点规模固定为这三家；新增机构需扩展配置及初始化脚本。

三、核验范围及结果
身份键为行业、数据范围、统一企业编码和年份。
仅比较相同数据范围内的企业年度记录，不交叉比较历史与待评估记录。
比较企业类型、通用指标、行业专项指标、环保评级；航空行业还比较细分类型。
不比较模型评分、授信标签、银行名称和数据库自增ID。
企业身份来自company表的唯一映射；无法唯一映射的本行记录跳过并展示数量。
数值忽略小数位数差异，空字符串视为缺失，但缺失与零不同。
重复的同企业年度记录完全相同时合并，有冲突时显示“重复记录待核查”。

浏览器仅接收本行已知的企业、年份、对方银行和以下状态：
CONSISTENT：指标一致
DIFFERENT：指标不一致
AMBIGUOUS：同企业年度存在冲突的重复记录
返回差异字段名称，不返回对方指标值、数据库ID或非共同客户。
双方集合大小属于协议所需元数据。空集合用随机占位元素处理，不映射成企业。
每次协议使用新密钥，输入顺序随机打乱，不复用稳定的企业明文摘要。

四、文件说明
node.py：银行节点API，区分管理员与对等银行凭据，校验数据范围、限流及错误处理。
serve.py：指定银行的Windows启动入口，只接受bank_a/b/c。
protocol.py：通过进程内管道调用Java引擎，限制输入和消息长度，90秒超时清理。
records.py：本行数据规范化、身份分组、重复歧义与三态输出。
provision.py：生成只读视图、localhost随机账号、节点配置和网关配置。
java/pom.xml：独立Maven模块，不修改主后端依赖树。
java/src/main/java/com/transfinance/psi/Engine.java：调用官方HFH99协议及RPC管道适配。
java/target/psi-engine.jar：构建出的可分发Java引擎。
requirements.txt：Python节点依赖，PyMySQL RSA扩展支持MySQL默认认证方式。
test_records.py：规范化和结果字段测试。
test_protocol.py：真实Java协议测试，包含空集、无交集、密文随机性和异常密文。
verify_nodes.py：本机验收工具，验证权限并对照实际协议结果；不属于节点业务路径。
../scripts/start-psi-nodes.ps1：初始化并启动节点，端口被其他程序占用时拒绝抢占。
../scripts/stop-psi-nodes.ps1：校验PID、启动时间和命令行后停止本模块进程。
../scripts/build-psi-engine.ps1：固定上游提交与归档校验和，可重复构建。
.runtime/psi位于项目根目录：存储生成的凭据、PID记录及诊断日志，仅当前用户与SYSTEM可访问。

五、打包给别人
包含psi-service源码、java/target/psi-engine.jar、启动停止bat和对应scripts。
不需要发送psi-service/.vendor源码缓存或java/target的其他构建中间文件。
不要发送.runtime/psi；它含本机账号密码及节点凭据，必须在接收方机器重新生成。
Python虚拟环境应在对方电脑重新创建，不直接复制本机.venv。
数据库连接配置按对方本机填写，不会自动远程连接发送者电脑。
保留本目录第三方许可证和说明。源代码部署需联网构建，已带引擎JAR可跳过Maven构建。

六、验证命令
在psi-service目录运行：
  ..\ml-service\.venv\Scripts\python.exe -m unittest test_records.py test_protocol.py
在根目录运行（需已启动三个节点）：
  .\ml-service\.venv\Scripts\python.exe psi-service\verify_nodes.py
在backend目录运行网关真实节点联调（需节点已启动）：
  mvn test -Dpsi.integration=true
验收脚本只输出结果数量和通过状态，不输出企业明细或凭据。
管理员只读对照查询仅存在于验收工具中，生产节点绝不使用管理员账号。

七、安全边界
采用半诚实双方模型，是单机模拟，不是恶意安全、生产银行级部署。
相同Windows用户可访问本机进程及配置；数据库管理员也能访问源数据。
数据库账号隔离不等于宿主机进程的操作系统级沙箱隔离。
求交会披露共同关系、一致性、差异字段名称和集合规模，不能声称零泄露。
逐字段核验只提交本方已有值的加密输入，不返回对方取值或普通摘要。
数值按规范化后比较（1与1.00一致），缺失与0不同，文本和布尔值也参与比较。
重复记录存在冲突时无法唯一定位差异字段，显示待核查。评分与授信标签不参与比较。
字段协议升级后须重新启动三个PSI节点和后端；不同版本或字段定义的节点拒绝核验。
当前协议哈希具有可忽略的碰撞概率，不作绝对零误差承诺。
真实跨机构部署需独立宿主机、TLS、机构认证、审计及安全评审。
新的PSI接口使用服务端登录会话；不代表旧业务API已全部完成权限改造。
任务采用同步调用，任何对等节点失败都会报告整次失败，不展示部分结果。

八、上游与许可证
MPC4J（研究库，Apache-2.0）：
https://github.com/alibaba-edu/mpc4j
版本v1.1.5，提交ea3b9aa99389adbe6219b0b909d2cc39cacad2bd。
源码归档SHA256：
954F9947F9B1EA9C574063F39DF17BB3C8C0359C46E7C9907BCD93A673427B10
官方源码未修改，应用层仅实现传输适配。许可证见MPC4J-LICENSE.txt。
构建依赖的各自许可证随JAR中的META-INF许可文件保留。
