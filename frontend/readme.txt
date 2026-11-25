前端目录说明

1. 目录作用
本目录是《企业转型金融数据平台》的 Vue 前端，负责：
- 金融机构端登录、注册、后台管理界面
- 企业端登录、注册、历史数据、待评估数据、待补全数据界面
- 模型训练、联邦学习、报告预览等页面
- 调用后端 REST API

2. 技术栈
- Vue 3
- Vite
- Element Plus
- ECharts
- Axios

3. 目录结构说明
- `package.json`
  - 前端依赖与启动脚本。
- `vite.config.js`
  - Vite 构建配置。
- `index.html`
  - 前端应用入口 HTML。
- `src/main.js`
  - Vue 应用入口，负责挂载路由和全局样式。
- `src/App.vue`
  - 顶层应用容器。
- `src/router/index.js`
  - 路由配置，区分金融机构端和企业端页面。
- `src/api/`
  - 前端接口封装。
  - 每个 `*.js` 文件对应一类后端接口。
- `src/components/`
  - 复用组件，例如报告弹窗、雷达图悬浮预览、折线图预览。
- `src/utils/`
  - 通用工具，例如行业选项、雷达图配置、颜色风格判断。
- `src/views/`
  - 页面级组件，承载所有业务界面。
- `src/styles/global.css`
  - 全局样式。

4. 关键接口文件说明
- `src/api/request.js`
  - Axios 实例封装、统一响应处理、统一错误提示。
- `src/api/auth.js`
  - 金融机构登录注册接口。
- `src/api/companyAuth.js`
  - 企业登录注册、资料修改接口。
- `src/api/companyData.js`
  - 企业端历史/待评估数据接口。
- `src/api/electricCompanyData.js`
  - 金融机构端评估数据管理接口。
- `src/api/pendingCompanyData.js`
  - 待评估数据、评分、授信、删除接口。
- `src/api/modelTraining.js`
  - 本地模型训练相关接口。
- `src/api/federatedLearning.js`
  - 联邦学习相关接口。
- `src/api/assessmentReports.js`
  - 报告生成接口。

5. 关键页面说明
- `src/views/LoginView.vue`
  - 金融机构登录页。
- `src/views/RegisterView.vue`
  - 金融机构注册页。
- `src/views/DashboardLayout.vue`
  - 金融机构端后台布局。
- `src/views/ElectricCompanyDataView.vue`
  - 金融机构端企业评估数据列表、新增、修改、删除、评分。
- `src/views/PendingCompanyDataView.vue`
  - 待评估数据、评分、授信、报告、删除。
- `src/views/ModelTrainingView.vue`
  - 本机构模型训练设置页。
- `src/views/FederatedLearningView.vue`
  - 联邦学习设置页。
- `src/views/CompanyLoginView.vue`
  - 企业登录页。
- `src/views/CompanyRegisterView.vue`
  - 企业注册页。
- `src/views/CompanyDashboardLayout.vue`
  - 企业端后台布局。
- `src/views/CompanyAssessmentDataView.vue`
  - 企业端历史数据与待评估数据页。
- `src/views/CompanyMissingDataView.vue`
  - 企业待补全数据页。
- `src/views/FinancialDataNoticeView.vue`
  - 企业收到的补全提醒与通知列表。

6. 组件说明
- `src/components/AssessmentReportDialog.vue`
  - 评估报告弹窗。
- `src/components/RadarHoverPreview.vue`
  - 雷达图悬浮预览。
- `src/components/TransitionScoreTrendHoverPreview.vue`
  - 历史趋势折线图预览。

7. 启动方式
- 在 `frontend` 目录执行 `npm run dev`
- 默认开发地址：`http://localhost:5173`

8. 交付注意事项
- `dist/` 是构建产物，不需要手工修改。
- `node_modules/` 只用于本地开发，不建议打包提交。
- 如果后端地址变化，需要同步检查 `src/api/request.js` 或相关环境配置。
