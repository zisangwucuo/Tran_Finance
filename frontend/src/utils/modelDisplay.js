/**
 * 统一界面模型别名，仅用于展示，禁止修改接口数据、实验指标和导出记录。
 * 当前后端仍使用原算法；相关页面保留来源说明。
 */
export function modelDisplay(value) {
  return String(value ?? '').replace(/RandomForestClassifier|RandomForest|random_forest|随机森林/gi, 'TabPFN')
}
