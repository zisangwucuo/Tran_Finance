<template>
  <div class="federated-page">
    <section class="hero-band">
      <div class="hero-copy">
        <p>横向联邦学习</p>
        <h2>跨机构联合建模</h2>
        <span>TabPFN 尚未接入；当前联邦任务和结果仍使用后端原有模型，不汇集企业原始数据。</span>
      </div>
      <div class="action-row">
        <el-button :loading="training" :icon="DataAnalysis" type="primary" @click="handleTrain">
          一键联邦训练
        </el-button>
        <el-button :loading="clearing" :icon="Delete" type="danger" plain @click="handleClear">
          清空联邦模型
        </el-button>
        <el-button :icon="RefreshRight" @click="loadSummary">刷新状态</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div class="summary-panel">
        <h3>联邦训练任务</h3>
        <div class="summary-line">状态：{{ financialModelStatus(latestJob?.status) }}</div>
        <div class="summary-line">消息：{{ modelDisplay(latestJob?.message) || '-' }}</div>
        <el-progress :percentage="latestJob?.progress || 0" :status="progressStatus" />
        <div class="meta-row">
          <span>已聚合 {{ latestJob?.trainedModels || 0 }}/{{ latestJob?.totalModels || 0 }} 个行业</span>
          <span>{{ formatDate(latestJob?.createdAt) }}</span>
        </div>
      </div>

      <div class="summary-panel">
        <h3>聚合概览</h3>
        <div class="metric-grid">
          <div>
            <strong>{{ modelCount }}</strong>
            <span>行业模型</span>
          </div>
          <div>
            <strong>{{ participantCount }}</strong>
            <span>参与机构</span>
          </div>
          <div>
            <strong>{{ sampleCount }}</strong>
            <span>聚合样本</span>
          </div>
          <div>
            <strong>{{ yearRange }}</strong>
            <span>年份区间</span>
          </div>
        </div>
      </div>

      <div class="summary-panel">
        <h3>训练机制</h3>
        <div class="summary-line">每家金融机构先保留自己的本地行业模型。</div>
        <div class="summary-line">联邦模型按行业收集各机构模型，并按训练样本数加权平均预测概率。</div>
        <div class="summary-line">当前指标权重来自原有本地模型特征重要性的加权聚合，不代表 TabPFN 结果。</div>
      </div>
    </section>

    <section class="model-band">
      <div class="table-head">
        <h3>行业联邦模型</h3>
        <el-tag effect="plain" type="info">共 {{ models.length }}/8 个行业</el-tag>
      </div>

      <div v-if="models.length" class="federated-grid">
        <article v-for="model in models" :key="model.industryKey" class="federated-card">
          <div class="model-card-head">
            <div>
              <h4>{{ model.industryName || '-' }}</h4>
              <span>{{ modelDisplay(model.modelType) || '-' }}</span>
            </div>
            <el-tag :type="model.status === 'READY' ? 'success' : 'info'" effect="plain">
              {{ formatStatus(model.status) }}
            </el-tag>
          </div>

          <div class="model-facts">
            <span>机构 {{ model.participantCount || 0 }}</span>
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
            <strong>聚合权重前三指标</strong>
            <span v-for="item in formatTopWeights(model.featureWeightsJson)" :key="item.prop">
              {{ item.label }} {{ item.percent }}
            </span>
            <span v-if="!formatTopWeights(model.featureWeightsJson).length">暂无权重</span>
          </div>

          <div class="participant-row">
            <strong>参与机构</strong>
            <span v-for="item in formatParticipants(model.participantSummaryJson)" :key="item.institutionCode">
              {{ item.institutionName }} {{ item.percent }}
            </span>
          </div>

          <div class="model-footer">
            <span>发起机构：{{ model.trainedBy || '-' }}</span>
            <span>{{ formatDate(model.trainedAt) }}</span>
          </div>
        </article>
      </div>

      <el-empty v-else description="暂无联邦行业模型" />
    </section>
  </div>
</template>

<script setup>
import { financialModelStatus } from '../utils/financialNavigation'
import { modelDisplay } from '../utils/modelDisplay'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Delete, RefreshRight } from '@element-plus/icons-vue'
import {
  clearFederatedModels,
  fetchFederatedLearningSummary,
  trainFederatedModels
} from '../api/federatedLearning'
import { comprehensiveIndicators, specialtyIndicators } from '../utils/radarConfig'

const router = useRouter()
const currentUser = computed(() => {
  const raw = localStorage.getItem('financialUser')
  return raw ? JSON.parse(raw) : null
})

const training = ref(false)
const clearing = ref(false)
const summary = ref({ latestJob: null, models: [] })
let pollingTimer = null
const indicatorLabelMap = buildIndicatorLabelMap()

// 联邦页面同样使用一个任务对象描述进度，前端据此决定是否轮询刷新。
const latestJob = computed(() => summary.value.latestJob)
const models = computed(() => summary.value.models || [])
const modelCount = computed(() => models.value.length)
const participantCount = computed(() => {
  const codes = new Set()
  models.value.forEach((model) => {
    formatParticipants(model.participantSummaryJson).forEach((item) => codes.add(item.institutionCode))
  })
  return codes.size
})
const sampleCount = computed(() => models.value.reduce((sum, item) => sum + Number(item.sampleCount || 0), 0))
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
  // 联邦训练和本机构训练共用同一套任务轮询体验，这里先读最新状态。
  summary.value = await fetchFederatedLearningSummary()
  const status = summary.value.latestJob?.status
  if (status === 'PENDING' || status === 'RUNNING') {
    startPolling()
  } else {
    stopPolling()
  }
}

async function handleTrain() {
  await ElMessageBox.confirm(
    '确认发起原有后端模型的联邦集成训练吗？此操作不使用 TabPFN，会聚合同一行业下各金融机构已经训练完成的本地模型，不传输企业原始数据。',
    '联邦训练确认',
    {
      confirmButtonText: '开始训练',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  training.value = true
  try {
    // 联邦训练只聚合已经训练好的本地随机森林，不读取企业原始样本。
    await trainFederatedModels(currentUser.value.institutionId)
    ElMessage.success('联邦学习任务已提交')
    await loadSummary()
  } finally {
    training.value = false
  }
}

async function handleClear() {
  await ElMessageBox.confirm('确认清空已经生成的全部联邦模型吗？本地机构模型不会被删除。', '清空联邦模型确认', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  clearing.value = true
  try {
    // 这里只清联邦模型元数据和文件，不影响各金融机构自己的本地模型。
    const result = await clearFederatedModels()
    ElMessage.success(`已清空 ${result?.deletedModels || 0} 条联邦模型记录`)
    await loadSummary()
  } finally {
    clearing.value = false
  }
}

function startPolling() {
  if (pollingTimer) {
    return
  }
  // 联邦训练同样是后台任务，所以轮询间隔短一点，方便页面更快反馈结果。
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

function formatStatus(value) {
  if (value === 'READY') return '就绪'
  if (value === 'FAILED') return '失败'
  if (value === 'RUNNING') return '训练中'
  return value || '-'
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

function formatParticipants(value) {
  if (!value) return []
  try {
    const participants = typeof value === 'string' ? JSON.parse(value) : value
    if (!Array.isArray(participants)) {
      return []
    }
    return participants.map((item) => ({
      institutionCode: item.institutionCode,
      institutionName: item.institutionName || item.institutionCode,
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
.federated-page {
  display: grid;
  gap: 16px;
  width: 100%;
  min-width: 0;
  padding: 20px;
  overflow-x: hidden;
}

.hero-band,
.summary-panel,
.model-band {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.hero-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.hero-copy {
  display: grid;
  gap: 6px;
}

.hero-copy p {
  margin: 0;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.hero-copy h2,
.summary-panel h3,
.table-head h3 {
  margin: 0;
  color: #23364f;
  font-size: 18px;
  font-weight: 800;
}

.hero-copy span,
.summary-line {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.action-row,
.table-head,
.meta-row,
.model-card-head,
.model-facts,
.model-metrics,
.weight-row,
.participant-row,
.model-footer {
  display: flex;
}

.action-row {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(280px, 0.9fr) minmax(320px, 1.1fr);
  gap: 14px;
  min-width: 0;
}

.summary-panel {
  min-width: 0;
  padding: 16px 18px;
}

.summary-line {
  margin-top: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(110px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.metric-grid div {
  min-width: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.metric-grid strong {
  display: block;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.metric-grid span,
.meta-row,
.model-card-head span,
.model-footer {
  color: #64748b;
  font-size: 12px;
}

.meta-row,
.model-footer {
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.model-band {
  min-width: 0;
  padding: 16px 18px 18px;
}

.table-head {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.federated-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
  gap: 12px;
}

.federated-card {
  min-width: 0;
  padding: 14px;
  background: #fbfdff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.model-card-head {
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

.model-facts,
.model-metrics,
.weight-row,
.participant-row {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.model-facts span,
.weight-row span,
.participant-row span {
  max-width: 100%;
  padding: 3px 8px;
  overflow: hidden;
  border-radius: 4px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-facts span {
  color: #475569;
  background: #eef4fb;
}

.weight-row strong,
.participant-row strong {
  flex-basis: 100%;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.weight-row span {
  color: #2563eb;
  background: #eff6ff;
}

.participant-row span {
  color: #047857;
  background: #ecfdf5;
}

.model-footer {
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-band {
    align-items: flex-start;
    flex-direction: column;
  }

  .action-row {
    justify-content: flex-start;
  }
}
</style>
