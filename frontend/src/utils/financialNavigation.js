import { FolderOpened, MessageBox, Cpu, Connection, Histogram, DocumentChecked, DataBoard, Refresh } from '@element-plus/icons-vue'

// 同一份菜单配置用于桌面侧栏、移动抽屉及面包屑，路由沿用原业务页面。
export const financialNavigation = [
  { title: '工作台', number: '00', items: [
    { path: '/dashboard/customer-overview', label: '客户总览', icon: DataBoard }
  ] },
  { title: '数据管理', number: '01', items: [
    { path: '/dashboard/electric-company-data', label: '历史评估档案', icon: FolderOpened },
    { path: '/dashboard/data-notices', label: '数据补全协同', icon: MessageBox }
  ] },
  { title: '模型管理', number: '02', items: [
    { path: '/dashboard/model-training', label: '本机构模型管理', icon: Cpu },
    { path: '/dashboard/federated-learning', label: '跨机构联合建模', icon: Connection },
    { path: '/dashboard/model-comparison', label: '模型效果对比', icon: Histogram }
  ] },
  { title: '评估业务', number: '03', items: [
    { path: '/dashboard/pending-company-data', label: '企业评估与授信', icon: DocumentChecked }
  ] },
  { title: '动态更新', number: '04', items: [
    { path: '/dashboard/dynamic-update', label: '动态更新', icon: Refresh }
  ] }
]

export function financialPage(path) {
  for (const group of financialNavigation) {
    const item = group.items.find(entry => entry.path === path)
    if (item) return { ...item, group: group.title }
  }
  return { label: '金融机构工作台', group: '工作台' }
}

// 仅翻译界面文本，任务轮询及按钮可用性仍比较接口原始状态码。
export function financialModelStatus(value) {
  return { PENDING: '等待训练', RUNNING: '训练中', SUCCESS: '已完成', FAILED: '失败', READY: '已就绪' }[value] || value || '未开始'
}
