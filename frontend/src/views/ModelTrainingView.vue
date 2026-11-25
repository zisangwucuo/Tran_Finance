<template>
  <div class="training-page">
    <section class="top-band">
      <div class="page-title">
        <h2>本机构模型管理</h2>
        <span>{{ currentUser?.institutionName || currentUser?.institutionId || '当前金融机构' }}</span>
      </div>
      <div class="action-row">
        <el-button :disabled="trainingBusy" :loading="training" :icon="DataAnalysis" type="primary" @click="handleTrainAll">一键训练模型</el-button>
        <el-button :disabled="trainingBusy" :loading="updating || loadingParameters" :icon="Refresh" @click="openParameterDialog">更新模型</el-button>
        <el-button :loading="notifying" :icon="Bell" type="warning" @click="handleNotifyCompanies">通知企业补全</el-button>
        <el-button :loading="clearing" :icon="Delete" type="danger" plain @click="handleClearModels">清空模型</el-button>
        <el-button :icon="RefreshRight" @click="loadSummary">刷新状态</el-button>
      </div>
    </section>

    <el-alert title="TabPFN 为前端显示名，后端算法尚未替换；当前指标与结果来自原算法。" type="info" :closable="false" />
    <section class="summary-grid">
      <div class="summary-card">
        <h3>训练任务</h3>
        <div class="summary-line">状态：{{ financialModelStatus(latestJob?.status) }}</div>
        <div class="summary-line">任务类型：{{ latestJob?.jobType || '-' }}</div>
        <div class="summary-line">消息：{{ modelDisplay(latestJob?.message) || '-' }}</div>
        <el-progress :percentage="latestJob?.progress || 0" :status="progressStatus" />
        <div class="meta-row">
          <span>已训练 {{ latestJob?.trainedModels || 0 }}/{{ latestJob?.totalModels || 0 }} 个模型</span>
          <span>创建时间：{{ formatDate(latestJob?.createdAt) }}</span>
        </div>
      </div>

      <div class="summary-card">
        <h3>训练集概览</h3>
        <div class="summary-line">模型总数：{{ modelCount }}</div>
        <div class="summary-line">覆盖行业：{{ industryCount }}</div>
        <div class="summary-line">目标变量：经核验转型绩效</div>
        <div class="summary-line">训练样本：{{ sampleCount }}</div>
        <div class="summary-line">企业数量：{{ companyCount }}</div>
        <div class="summary-line">年份区间：{{ yearRange }}</div>
      </div>

      <div class="summary-card">
        <h3>运行说明</h3>
        <div class="summary-line">每家金融机构按八个行业分别训练八个模型。</div>
        <div class="summary-line">目标变量为经核验转型绩效，每个行业使用通用指标和本行业专项指标。</div>
        <div class="summary-line">训练时剔除各行业最新年份，模型输出概率后乘以 100 写入转型综合评分。</div>
        <div class="summary-line">训练完成后保存特征权重，为后续指标贡献度分析提供依据。</div>
      </div>
    </section>

    <section class="table-band">
      <div class="table-head">
        <h3>模型状态</h3>
        <el-tag effect="plain" type="info">共 {{ models.length }}/8 个行业模型</el-tag>
      </div>
      <div v-if="models.length" class="model-grid">
        <article v-for="model in models" :key="`${model.industryKey}-${model.targetName}`" class="model-card">
          <div class="model-card-head">
            <div>
              <h4>{{ model.industryName || '-' }}</h4>
              <span>{{ formatTargetName(model.targetName) }}</span>
            </div>
            <el-tag :type="model.status === 'READY' ? 'success' : 'info'" effect="plain">{{ financialModelStatus(model.status) }}</el-tag>
          </div>
          <div class="model-facts">
            <span>样本 {{ model.sampleCount || 0 }}</span>
            <span>企业 {{ model.companyCount || 0 }}</span>
            <span>{{ model.yearStart || '-' }} - {{ model.yearEnd || '-' }}</span>
          </div>
          <div class="model-metrics">
            <el-tag
              v-for="item in formatMetricItems(model.metricsJson)"
              :key="item.label"
              effect="plain"
              size="small"
            >
              {{ item.label }} {{ item.value }}
            </el-tag>
          </div>
          <div class="weight-row">
            <strong>权重前三指标</strong>
            <el-tooltip
              v-for="item in formatTopWeights(model.featureWeightsJson)"
              :key="item.prop"
              :content="item.label"
              placement="top"
            >
              <span>{{ item.label }} {{ item.percent }}</span>
            </el-tooltip>
            <span v-if="!formatTopWeights(model.featureWeightsJson).length">暂无权重</span>
          </div>
          <div class="model-footer">
            <span>{{ modelDisplay(model.modelType) || '-' }}</span>
            <span>{{ formatDate(model.trainedAt) }}</span>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无行业模型" />
    </section>
    <el-dialog v-model="parameterDialogVisible" title="TabPFN 模型参数" width="min(560px, calc(100vw - 32px))" :close-on-click-modal="false" :close-on-press-escape="!updating" :show-close="!updating" class="model-parameter-dialog">
      <el-form label-position="top" :disabled="updating">
        <div class="parameter-grid">
          <el-form-item v-for="item in numericParameters" :key="item.key" :label="item.label">
            <el-input-number v-model="parameters[item.key]" :min="item.min" :max="item.max" :precision="0" :step="item.step || 1" controls-position="right" />
          </el-form-item>
          <el-form-item label="推理设备 device">
            <el-select v-model="parameters.device">
              <el-option label="自动选择" value="auto" />
              <el-option label="CPU" value="cpu" />
              <el-option label="CUDA" value="cuda" />
            </el-select>
          </el-form-item>
          <el-form-item label="概率温度 softmax_temperature">
            <el-input-number v-model="parameters.softmaxTemperature" :min="0.1" :max="2" :step="0.1" :precision="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="类别概率平衡 balance_probabilities">
            <el-switch v-model="parameters.balanceProbabilities" />
          </el-form-item>
          <el-form-item label="Softmax前平均 average_before_softmax">
            <el-switch v-model="parameters.averageBeforeSoftmax" />
          </el-form-item>        </div>
      </el-form>
      <template #footer>
        <el-button :disabled="updating" @click="parameters = defaultParameters()">恢复默认</el-button>
        <el-button :disabled="updating" @click="parameterDialogVisible = false">取消</el-button>
        <el-button type="primary">更新 TabPFN 模型</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { financialModelStatus } from '../utils/financialNavigation'
import { modelDisplay } from '../utils/modelDisplay'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, DataAnalysis, Delete, Refresh, RefreshRight } from '@element-plus/icons-vue'
import {
  clearModels,
  fetchModelTrainingSummary,
  notifyCompaniesForMissingData,
  trainAllModels,
  updateModels
} from '../api/modelTraining'
import { industryOptions } from '../utils/industryOptions'
import { comprehensiveIndicators, specialtyIndicators } from '../utils/radarConfig'

const router = useRouter()
const currentUser = computed(() => {
  const raw = localStorage.getItem('financialUser')
  return raw ? JSON.parse(raw) : null
})

const training = ref(false)
const updating = ref(false)
const notifying = ref(false)
const clearing = ref(false)
// 展示用TabPFN配置，不发送给当前后端；实际可用参数取决于未来接入版本。
const defaultParameters = () => ({ nEstimators: 8, randomState: 42, device: 'auto',
  softmaxTemperature: 0.9, balanceProbabilities: false, averageBeforeSoftmax: false })
const parameters = ref(defaultParameters())
const parameterDialogVisible = ref(false)
const loadingParameters = ref(false)
const numericParameters = [
  { key: 'nEstimators', label: '集成预测次数 n_estimators', min: 1, max: 32 },
  { key: 'randomState', label: '随机种子 random_state', min: 0, max: 2147483647 }
]
const trainingBusy = computed(() => training.value || updating.value
  || ['PENDING', 'RUNNING'].includes(summary.value.latestJob?.status))
const summary = ref({ latestJob: null, models: [] })
let pollingTimer = null
const indicatorLabelMap = buildIndicatorLabelMap()

// 只保留“各行业本地转型贷款模型”，把联邦占位记录和其他目标变量过滤掉。
const models = computed(() => (summary.value.models || [])
  .filter((item) => item.industryKey !== 'all' && item.targetName === 'verified_transition_performance'))
const latestJob = computed(() => {
  const job = summary.value.latestJob
  if (!job) {
    return null
  }
  if (Number(job.totalModels) === 1 && models.value.length === 0) {
    return null
  }
  return job
})
const modelCount = computed(() => models.value.length)
const industryCount = computed(() => new Set(models.value.map((item) => item.industryKey)).size)
const sampleCount = computed(() => models.value.reduce((sum, item) => sum + Number(item.sampleCount || 0), 0))
const companyCount = computed(() => models.value.reduce((sum, item) => sum + Number(item.companyCount || 0), 0))
const yearRange = computed(() => {
  const years = models.value.flatMap((item) => [item.yearStart, item.yearEnd]).filter(Boolean)
  if (!years.length) {
    return '-'
  }
  return `${Math.min(...years)} - ${Math.max(...years)}`
})
const progressStatus = computed(() => {
  const status = latestJob.value?.status
  if (status === 'FAILED') return 'exception'
  if (status === 'SUCCESS') return 'success'
  return undefined
})

onMounted(async () => {
  if (!currentUser.value?.institutionId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await loadSummary()
})

onUnmounted(() => {
  stopPolling()
})

async function loadSummary() {
  // 页面刷新后先拿最新任务和模型列表，再决定是否开启轮询。
  summary.value = await fetchModelTrainingSummary(currentUser.value.institutionId)
  const status = summary.value.latestJob?.status
  if (status === 'PENDING' || status === 'RUNNING') {
    startPolling()
  } else {
    stopPolling()
  }
}

async function handleTrainAll() {
  training.value = true
  try {
    // 一键训练会在后端依次训练八个行业模型，前端只负责提交任务和刷新状态。
    await submitTraining(false, false)
  } catch (error) {
    try {
      await confirmForceTraining(error, false)
    } catch {
      // 请求拦截器已经提示了错误，避免二次请求失败形成未处理的 Promise。
    }
  } finally {
    training.value = false
  }
}

// TabPFN参数仅作界面原型，不读取森林元数据，也不调用训练接口。
function openParameterDialog() {
  parameterDialogVisible.value = true
}
async function submitTraining(updateMode, force, parameterSnapshot) {
  // updateMode 决定是首次训练还是更新训练，force 则决定是否绕过缺失数据校验。
  if (updateMode) {
    await updateModels(currentUser.value.institutionId, force, parameterSnapshot)
    parameterDialogVisible.value = false
    ElMessage.success(force ? '强行更新任务已提交' : '更新任务已提交')
  } else {
    await trainAllModels(currentUser.value.institutionId, force)
    ElMessage.success(force ? '强行训练任务已提交' : '训练任务已提交')
  }
  await loadSummary()
}

async function confirmForceTraining(error, updateMode, parameterSnapshot) {
  const message = extractErrorMessage(error)
  if (!message.includes('缺失字段')) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '当前训练集仍存在缺失字段。是否强行训练模型？强行训练会把缺失值按模型服务当前规则处理，可能影响评分准确性。',
      '强行训练确认',
      {
        confirmButtonText: '强行训练',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    ElMessage.info('已取消训练')
    return
  }
  await submitTraining(updateMode, true, parameterSnapshot)
}

function extractErrorMessage(error) {
  return error?.response?.data?.message
    || error?.response?.data?.data?.message
    || error?.message
    || ''
}

async function handleNotifyCompanies() {
  notifying.value = true
  try {
    // 训练前通知企业补全缺失字段，避免训练集存在大量空值。
    const result = await notifyCompaniesForMissingData(currentUser.value.institutionId)
    const missingRows = Number(result?.missingRows || 0)
    if (missingRows > 0) {
      ElMessage.success(`已通知企业补全，共 ${missingRows} 条缺失数据`)
    } else {
      ElMessage.success('未发现缺失训练数据，无需通知企业')
    }
  } finally {
    notifying.value = false
  }
}

async function handleClearModels() {
  await ElMessageBox.confirm('确认清空当前金融机构已经训练好的模型吗？清空后需要重新训练才能开始评估。', '清空模型确认', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  clearing.value = true
  try {
    // 清空模型只删除当前机构的本地模型与训练任务，不影响其他机构。
    const result = await clearModels(currentUser.value.institutionId)
    ElMessage.success(`已清空 ${result?.deletedModels || 0} 条模型记录`)
    await loadSummary()
  } finally {
    clearing.value = false
  }
}

function startPolling() {
  if (pollingTimer) {
    return
  }
  // 训练任务是异步后台执行的，所以这里每秒拉一次最新任务进度。
  pollingTimer = window.setInterval(() => {
    loadSummary().catch(() => {})
  }, 1000)
}

function stopPolling() {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

function formatMetrics(value) {
  const items = formatMetricItems(value)
  return items.length ? items.map((item) => `${item.label} ${item.value}`).join(' | ') : '-'
}

function formatMetricItems(value) {
  if (!value) return []
  try {
    const metrics = typeof value === 'string' ? JSON.parse(value) : value
    const parts = []
    if (metrics.accuracy !== undefined && metrics.accuracy !== null) parts.push({ label: '准确率', value: metrics.accuracy })
    if (metrics.auc !== undefined && metrics.auc !== null) parts.push({ label: 'AUC', value: metrics.auc })
    if (metrics.logLoss !== undefined && metrics.logLoss !== null) parts.push({ label: 'LogLoss', value: metrics.logLoss })
    return parts
  } catch {
    return []
  }
}

function formatTargetName(value) {
  return value === 'verified_transition_performance' ? '经核验转型绩效' : (value || '-')
}

function formatFeatureWeights(value) {
  const items = formatTopWeights(value)
  return items.length ? items.map((item) => `${item.prop} ${item.percent}`).join('、') : '-'
}

function formatTopWeights(value) {
  if (!value) return []
  try {
    const weights = typeof value === 'string' ? JSON.parse(value) : value
    if (!Array.isArray(weights) || !weights.length) {
      return []
    }
    return weights
      .slice()
      .sort((left, right) => Number(right.weight || 0) - Number(left.weight || 0))
      .slice(0, 3)
      .map((item) => ({
        prop: item.prop,
        label: indicatorLabelMap.get(item.prop) || item.prop,
        percent: `${(Number(item.weight || 0) * 100).toFixed(1)}%`
      }))
  } catch {
    return []
  }
}

function buildIndicatorLabelMap() {
  const entries = new Map()
  comprehensiveIndicators.forEach((item) => {
    entries.set(item.prop, item.label)
  })
  Object.values(specialtyIndicators).flat().forEach((item) => {
    if (!entries.has(item.prop)) {
      entries.set(item.prop, item.label)
    }
  })
  return entries
}
</script>

<style scoped>
.parameter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}
.parameter-grid :deep(.el-input-number) { width: 100%; }
@media (max-width: 600px) {
  .parameter-grid { grid-template-columns: minmax(0, 1fr); }
}
.training-page {
  display: grid;
  gap: 16px;
  padding: 20px;
  width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

.top-band,
.summary-card,
.table-band {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.top-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
}

.page-title h2,
.table-head h3,
.summary-card h3 {
  margin: 0;
  color: #23364f;
  font-size: 17px;
  font-weight: 800;
}

.page-title span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(260px, 0.9fr) minmax(300px, 1fr);
  gap: 14px;
  min-width: 0;
}

.summary-card {
  min-width: 0;
  padding: 16px 18px;
}

.summary-line {
  margin-top: 10px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  color: #64748b;
  font-size: 12px;
}

.table-band {
  min-width: 0;
  padding: 16px 18px 18px;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.model-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fbfdff;
}

.model-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.model-card-head h4 {
  margin: 0;
  color: #23364f;
  font-size: 15px;
  font-weight: 800;
}

.model-card-head span,
.model-footer {
  color: #64748b;
  font-size: 12px;
}

.model-facts,
.model-metrics,
.weight-row,
.model-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.weight-row strong {
  flex-basis: 100%;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.model-facts span {
  padding: 3px 8px;
  color: #475569;
  background: #eef4fb;
  border-radius: 4px;
  font-size: 12px;
}

.weight-row span {
  max-width: 100%;
  padding: 3px 7px;
  overflow: hidden;
  color: #2563eb;
  background: #eff6ff;
  border-radius: 4px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-footer {
  justify-content: space-between;
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .top-band {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
