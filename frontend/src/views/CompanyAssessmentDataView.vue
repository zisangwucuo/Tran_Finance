<template>
  <div class="company-data-page">
    <section class="filter-strip">
      <div class="filter-intro">
        <span class="eyebrow">企业数据中心</span>
        <h2>{{ pageTitle }}</h2>
        <p>
          {{ '企业编码' }}
          <span class="intro-divider">·</span>
          {{ currentUser?.companyCode || '-' }}
        </p>
      </div>

      <el-form :model="filters" class="filter-form" label-width="64px">
        <el-form-item class="year-filter" label="年份">
          <el-select v-model="filters.dataYear" clearable placeholder="全部年份">
            <el-option v-for="year in yearOptions" :key="year" :label="year" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="workspace-band">
      <div class="workspace-header">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageDescription }}</p>
        </div>
        <div class="header-status">
          <el-tag :type="isPending ? 'warning' : 'success'" effect="plain">
            {{ isPending ? '评估结果自动同步' : '已归档评估记录' }}
          </el-tag>
          <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
          <el-button :icon="Refresh" :loading="loading" @click="loadData()">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="isPending"
        class="sync-alert"
        :closable="false"
        show-icon
        title="金融机构完成模型评估后，待评估列表会自动显示转型综合评分和改进建议。"
        type="info"
      />

      <el-table
        v-loading="loading"
        :data="records"
        border
        class="assessment-table"
        empty-text="暂无属于本企业的数据"
      >
        <el-table-column fixed="left" label="ID" prop="id" width="64" />
        <el-table-column label="金融机构" prop="institutionName" width="108" show-overflow-tooltip />
        <el-table-column label="行业" prop="industryName" width="116" show-overflow-tooltip />
        <el-table-column label="年份" prop="dataYear" width="72" />
        <el-table-column label="评估状态" width="132">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.assessmentStatus)" effect="plain">
              {{ row.assessmentStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="转型综合评分" width="126">
          <template #default="{ row }">
            <span :class="['value-tone', getTransitionScoreClass(row.transitionScore)]">
              {{ formatScore(row.transitionScore) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isPending" label="授信结果" width="132">
          <template #default="{ row }">
            <el-tag :type="row.verifiedTransitionPerformance === 1 ? 'success' : 'info'" effect="plain">
              {{ formatLoanResult(row.verifiedTransitionPerformance) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评估建议" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="row.transitionScore === null ? 'advice-muted' : 'advice-text'">
              {{ row.assessmentAdvice || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="104">
          <template #default="{ row }">
            <el-button :icon="View" link type="primary" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span class="refresh-note">
          <el-icon><Clock /></el-icon>
          {{ lastUpdated ? `最近同步：${formatTime(lastUpdated)}` : '等待同步' }}
        </span>
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, prev, pager, next, sizes, jumper"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="860px"
      class="company-detail-dialog"
    >
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="detailData">
          <div class="detail-summary">
            <div class="summary-main">
              <span class="summary-label">转型综合评分</span>
              <strong :class="['summary-score', getTransitionScoreClass(detailData.summary?.transitionScore)]">
                {{ formatScore(detailData.summary?.transitionScore) }}
              </strong>
            </div>
            <div class="summary-item">
              <span>评估状态</span>
              <el-tag :type="getStatusTagType(detailData.summary?.assessmentStatus)" effect="plain">
                {{ detailData.summary?.assessmentStatus || '-' }}
              </el-tag>
            </div>
            <div class="summary-item">
              <span>{{ isPending ? '结果' : '授信结果' }}</span>
              <strong>{{ isPending ? detailData.summary?.resultLabel || '-' : formatLoanResult(detailData.summary?.verifiedTransitionPerformance) }}</strong>
            </div>
          </div>

          <div class="detail-meta">
            <div>
              <span>金融机构</span>
              <strong>{{ detailData.summary?.institutionName || '-' }}</strong>
            </div>
            <div>
              <span>行业</span>
              <strong>{{ detailData.summary?.industryName || '-' }}</strong>
            </div>
            <div>
              <span>年份</span>
              <strong>{{ detailData.summary?.dataYear || '-' }}</strong>
            </div>
            <div>
              <span>企业类型</span>
              <strong>{{ detailData.summary?.companyType || '-' }}</strong>
            </div>
          </div>

          <section class="advice-section">
            <div class="section-title">
              <el-icon><ChatLineSquare /></el-icon>
              <h3>评估建议</h3>
            </div>
            <p class="advice-summary">
              {{ detailData.summary?.assessmentAdvice || '暂无评估建议' }}
            </p>
            <ul v-if="detailData.summary?.suggestions?.length" class="suggestion-list">
              <li v-for="suggestion in detailData.summary.suggestions" :key="suggestion">{{ suggestion }}</li>
            </ul>
          </section>

          <section class="fields-section">
            <div class="section-title">
              <el-icon><DataAnalysis /></el-icon>
              <h3>数据明细</h3>
            </div>
            <div class="field-grid">
              <div v-for="field in (detailData.fields || []).filter(item => item.prop !== 'companyName')" :key="field.prop" class="field-item">
                <span>{{ field.label }}</span>
                <strong :class="getDetailFieldClass(field)">
                  {{ formatDetailValue(field) }}
                </strong>
              </div>
            </div>
          </section>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatLineSquare, Clock, DataAnalysis, Refresh, Search, View } from '@element-plus/icons-vue'
import {
  fetchCompanyAssessmentData,
  fetchCompanyAssessmentDetail,
  fetchCompanyAssessmentOptions
} from '../api/companyData'
import { getTransitionScoreClass } from '../utils/valueTone'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const detailLoading = ref(false)
const records = ref([])
const total = ref(0)
const yearOptions = ref([])
const lastUpdated = ref(null)
const detailVisible = ref(false)
const detailData = ref(null)
let refreshTimer = null
let dataRequestVersion = 0
let optionsRequestVersion = 0

const currentUser = computed(() => {
  const raw = localStorage.getItem('companyUser')
  return raw ? JSON.parse(raw) : null
})

const isPending = computed(() => route.meta.datasetType === 'pending')
const datasetType = computed(() => (isPending.value ? 'pending' : 'history'))
const pageTitle = computed(() => (isPending.value ? '待评估数据' : '已评估历史数据'))
const pageDescription = computed(() => (
  isPending.value
    ? '查看金融机构尚未授信或刚完成评估的本企业数据'
    : '查看本企业已经完成评估并归档的历史记录'
))
const detailTitle = computed(() => {
  const summary = detailData.value?.summary
  if (!summary) {
    return '数据详情'
  }
  return `${summary.industryName || ''} · ${summary.dataYear || ''} 年度评估详情`
})

const filters = reactive({
  page: 1,
  size: 10,
  companyCode: '',
  dataYear: null
})

onMounted(async () => {
  if (!currentUser.value?.companyCode) {
    ElMessage.warning('请先登录企业系统')
    router.push('/company/login')
    return
  }
  filters.companyCode = currentUser.value.companyCode
  await initializeData()
  refreshTimer = window.setInterval(() => loadData(true), 10000)
})

watch(datasetType, async () => {
  if (!currentUser.value?.companyCode) {
    return
  }
  // 历史页和待评估页共用同一个组件，切换路由时要主动重置状态并重新拉取数据，
  // 否则旧请求返回后可能把当前页数据覆盖掉。
  dataRequestVersion += 1
  optionsRequestVersion += 1
  filters.page = 1
  filters.dataYear = null
  detailVisible.value = false
  detailData.value = null
  await initializeData()
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
})

async function initializeData() {
  try {
    const optionsLoaded = await loadYearOptions()
    if (!optionsLoaded) {
      records.value = []
      total.value = 0
      return false
    }
    await loadData()
    return true
  } catch {
    // 请求拦截器已经提示用户，这里只阻止页面生命周期产生未处理 Promise。
    return false
  }
}

async function loadYearOptions() {
  const requestVersion = ++optionsRequestVersion
  const requestedDatasetType = datasetType.value
  try {
    // 请求版本号用于区分“旧页面的返回值”和“当前页面的返回值”。
    const result = await fetchCompanyAssessmentOptions(datasetType.value, {
      companyCode: filters.companyCode
    })
    if (requestVersion !== optionsRequestVersion || requestedDatasetType !== datasetType.value) {
      return false
    }
    yearOptions.value = result.years || []
    return true
  } catch {
    if (requestVersion === optionsRequestVersion && requestedDatasetType === datasetType.value) {
      yearOptions.value = []
    }
    return false
  }
}

async function loadData(silent = false) {
  const requestVersion = ++dataRequestVersion
  const requestedDatasetType = datasetType.value
  if (!silent) {
    loading.value = true
  }
  try {
    // 轮询刷新时也会走这里，所以必须防止慢请求把新页面的数据覆盖回去。
    const result = await fetchCompanyAssessmentData(datasetType.value, buildQueryParams())
    if (requestVersion !== dataRequestVersion || requestedDatasetType !== datasetType.value) {
      return
    }
    records.value = result.page?.records || []
    total.value = Number(result.page?.total || 0)
    lastUpdated.value = new Date()
  } catch {
    if (requestVersion === dataRequestVersion && requestedDatasetType === datasetType.value && !silent) {
      records.value = []
      total.value = 0
    }
  } finally {
    if (!silent && requestVersion === dataRequestVersion) {
      loading.value = false
    }
  }
}

function handleSearch() {
  filters.page = 1
  loadData()
}

async function handleReset() {
  Object.assign(filters, {
    page: 1,
    size: 10,
    dataYear: null
  })
  await loadYearOptions()
  await loadData()
}

function handleSizeChange() {
  filters.page = 1
  loadData()
}

function buildQueryParams() {
  const params = { ...filters }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) {
      delete params[key]
    }
  })
  return params
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await fetchCompanyAssessmentDetail(
      datasetType.value,
      row.industryKey,
      row.id,
      { companyCode: filters.companyCode }
    )
  } catch {
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function getStatusTagType(status) {
  if (!status) {
    return 'info'
  }
  if (status.includes('待')) {
    return 'warning'
  }
  if (status.includes('授信')) {
    return 'success'
  }
  if (status.includes('暂无')) {
    return 'info'
  }
  return 'primary'
}

function formatScore(value) {
  if (value === null || value === undefined || value === '') {
    return '待评估'
  }
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue.toFixed(2)
}

function formatLoanResult(value) {
  if (value === 1) {
    return '符合经核验转型绩效'
  }
  if (value === 0) {
    return '不符合经核验转型绩效'
  }
  return '暂无记录'
}

function formatDetailValue(field) {
  if (field.value === null || field.value === undefined || field.value === '') {
    return '-'
  }
  if (field.type === 'boolean') {
    return Number(field.value) === 1 ? '是' : '否'
  }
  if (field.prop === 'transitionScore') {
    return formatScore(field.value)
  }
  return field.value
}

function getDetailFieldClass(field) {
  if (field.prop === 'transitionScore') {
    return ['value-tone', getTransitionScoreClass(field.value)]
  }
  return ''
}

function formatTime(value) {
  return value.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.company-data-page {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 20px;
}

.filter-strip,
.workspace-band {
  min-width: 0;
  background: #fff;
  border: 1px solid #e3e8ef;
  border-radius: 6px;
}

.filter-strip {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 22px;
  padding: 18px 20px 6px;
}

.filter-intro {
  padding-top: 2px;
}

.eyebrow {
  color: #2f80ed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.filter-intro h2,
.workspace-header h2 {
  margin: 7px 0 0;
  color: #23364f;
  font-size: 18px;
  font-weight: 800;
}

.filter-intro p,
.workspace-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.intro-divider {
  margin: 0 5px;
  color: #b6c3d1;
}

.filter-form {
  display: grid;
  grid-template-columns: 250px auto;
  gap: 8px 14px;
  align-items: start;
}

.filter-form :deep(.el-select) {
  width: 100%;
}

.year-filter :deep(.el-select) {
  width: 180px;
}

.filter-actions {
  min-width: 150px;
}

.workspace-band {
  padding: 18px 20px 16px;
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.sync-alert {
  margin-bottom: 12px;
}

.assessment-table {
  width: 100%;
  min-width: 0;
}

.value-tone {
  font-weight: 800;
}

.value-success {
  color: #16a34a;
}

.value-primary {
  color: #2563eb;
}

.value-warning {
  color: #d97706;
}

.value-danger {
  color: #dc2626;
}

.value-muted {
  color: #94a3b8;
}

.advice-text {
  color: #475569;
  line-height: 1.6;
}

.advice-muted {
  color: #94a3b8;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
}

.refresh-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.detail-content {
  min-height: 180px;
}

.detail-summary {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #eff6ff, #ecfeff);
  border: 1px solid #dbeafe;
  border-radius: 6px;
}

.summary-main,
.summary-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.summary-label,
.summary-item span,
.detail-meta span,
.field-item span {
  color: #64748b;
  font-size: 12px;
}

.summary-score {
  font-size: 30px;
  line-height: 1;
}

.summary-item strong {
  color: #23364f;
  font-size: 14px;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.detail-meta div {
  display: grid;
  gap: 5px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.detail-meta strong {
  color: #334155;
  font-size: 14px;
}

.advice-section,
.fields-section {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 7px;
}

.section-title .el-icon {
  color: #2f80ed;
}

.section-title h3 {
  margin: 0;
  color: #23364f;
  font-size: 15px;
}

.advice-summary {
  margin: 10px 0 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.75;
}

.suggestion-list {
  margin: 8px 0 0;
  padding-left: 20px;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.field-item {
  display: grid;
  gap: 6px;
  min-height: 54px;
  padding: 9px 11px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.field-item strong {
  color: #334155;
  font-size: 13px;
  line-height: 1.45;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .filter-strip {
    grid-template-columns: 1fr;
  }

  .filter-form {
    grid-template-columns: 250px auto;
  }
}

@media (max-width: 760px) {
  .company-data-page {
    padding: 12px;
  }

  .filter-form {
    grid-template-columns: 1fr;
  }

  .year-filter :deep(.el-select) {
    width: 100%;
  }

  .workspace-header,
  .table-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-status {
    justify-content: flex-start;
  }

  .detail-summary,
  .detail-meta,
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
