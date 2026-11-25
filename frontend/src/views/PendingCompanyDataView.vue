<template>
  <div class="pending-page" @click="closeContextMenu">
    <section class="filter-strip">
      <div class="filter-title">
        <h2>企业评估与授信</h2>
        <span>{{ currentUser?.institutionName || currentUser?.institutionId || '当前金融机构' }}</span>
      </div>
      <el-form :model="filters" class="filter-form" label-position="top">
        <el-form-item label="行业">
          <el-select v-model="filters.industryKey" placeholder="请选择行业" @change="handleIndustryChange">
            <el-option v-for="industry in industryOptions" :key="industry.key" :label="industry.name" :value="industry.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="filters.dataYear" clearable placeholder="请选择年份">
            <el-option v-for="year in yearOptions" :key="year" :label="year" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业编码">
          <el-input v-model.trim="filters.companyCode" clearable placeholder="请输入企业编码" />
        </el-form-item>
        <el-form-item label="股票代码">
          <el-input v-model.trim="filters.stockCode" clearable placeholder="请输入股票代码" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <FinancialDataSummary :items="dataSummary" :loading="loading" />

    <section class="workspace-band">
      <div class="workspace-header">
        <div>
          <h2>{{ activeIndustryName }} · 待评估清单</h2>
        </div>
        <div class="header-actions">
          <el-tag :type="isCurrentIndustryModelReady ? 'success' : 'info'" effect="plain">
            {{ isCurrentIndustryModelReady ? '本机构模型已就绪' : '本机构模型未就绪' }}
          </el-tag>
          <el-tag :type="isCurrentFederatedModelReady ? 'success' : 'info'" effect="plain">
            {{ isCurrentFederatedModelReady ? '联邦模型已就绪' : '联邦模型未就绪' }}
          </el-tag>
          <el-tag effect="plain" type="warning">待评估 {{ total }} 条</el-tag>
          <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        border
        class="data-table pending-table"
        @row-contextmenu="openContextMenu"
      >
        <el-table-column fixed="left" label="ID" prop="id" width="90" />
        <el-table-column
          v-for="column in tableColumns"
          :key="column.prop"
          :fixed="column.fixed || false"
          :label="column.label"
          :prop="column.prop"
          :width="column.width"
          :show-overflow-tooltip="column.prop !== 'companyCode'"
        >
          <template #default="{ row }">
            <el-tag v-if="column.type === 'boolean'" :type="row[column.prop] === 1 ? 'success' : 'info'">
              {{ formatBoolean(row[column.prop]) }}
            </el-tag>
            <span
              v-else-if="column.prop === 'companyCode'"
              class="company-code-cell"
              :class="{ 'company-review-mark': isMarked(row) }"
              @mouseenter="scheduleRadarPreview(row, $event)"
              @mousemove="moveRadarPreview($event)"
              @mouseleave="scheduleHideRadarPreview"
            >
              {{ formatTableCell(row.companyCode) }}
            </span>
            <span v-else :class="[getCellToneClass(column.prop, row[column.prop]), { 'company-review-mark': column.prop === 'companyName' && isMarked(row) }]">
              {{ formatTableCell(row[column.prop]) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="190">
          <template #default="{ row }">
            <el-button
              :disabled="!hasAnyScoringModelReady"
              :icon="DataAnalysis"
              :loading="scoringId === row.id"
              link
              type="primary"
              @click="handleStartScore(row)"
            >
              开始评估
            </el-button>
            <el-button :disabled="!hasTransitionScore(row)" :icon="DataAnalysis" link type="warning" @click="handleCredit(row)">授信</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          layout="total, prev, pager, next, sizes, jumper"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <RadarHoverPreview
      :industry-key="filters.industryKey"
      :row="radarPreview.row"
      :visible="radarPreview.visible"
      :x="radarPreview.x"
      :y="radarPreview.y"
      @mouseenter="keepRadarPreview"
      @mouseleave="scheduleHideRadarPreview"
    />

    <AssessmentReportDialog
      v-model="reportDialogVisible"
      :report="assessmentReport"
    />

    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
      @click.stop
    >
      <button type="button" @click="openEditDialog(contextMenu.row)">
        <el-icon><Edit /></el-icon>
        修改
      </button>
      <button type="button" class="danger" @click="handleDelete(contextMenu.row)">
        <el-icon><Delete /></el-icon>
        删除
      </button>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1120px" class="data-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-scrollbar max-height="68vh">
          <div class="dialog-grid">
            <section class="dialog-section">
              <h3 class="dialog-section-title">基础信息</h3>
              <div class="form-grid">
                <el-form-item label="行业类型" prop="industryKey">
                  <el-select v-model="dialogIndustryKey" placeholder="请选择行业类型" @change="handleDialogIndustryChange">
                    <el-option v-for="industry in industryOptions" :key="industry.key" :label="industry.name" :value="industry.key" />
                  </el-select>
                </el-form-item>
                <el-form-item label="企业编码" prop="companyCode">
                  <el-input v-model.trim="form.companyCode" />
                </el-form-item>
                <el-form-item label="股票代码" prop="stockCode">
                  <el-input
                    v-model.trim="form.stockCode"
                    :disabled="form.companyType === '非上市企业'"
                    :placeholder="stockCodePlaceholder"
                  />
                </el-form-item>
                <el-form-item label="企业类型" prop="companyType">
                  <el-select v-model="form.companyType" placeholder="请选择企业类型" @change="handleCompanyTypeChange">
                    <el-option label="上市企业" value="上市企业" />
                    <el-option label="非上市企业" value="非上市企业" />
                  </el-select>
                </el-form-item>
                <el-form-item label="年份" prop="dataYear">
                  <el-input-number v-model="form.dataYear" :min="2000" :max="2100" />
                </el-form-item>
              </div>
            </section>

            <section class="dialog-section">
              <h3 class="dialog-section-title">通用评估指标</h3>
              <div class="form-grid">
                <template v-for="column in governanceColumns" :key="column.prop">
                  <el-form-item :label="column.label" :prop="column.prop">
                    <component :is="resolveEditor(column)" v-model="form[column.prop]" v-bind="resolveEditorProps(column)" />
                  </el-form-item>
                </template>
              </div>
            </section>

            <section v-if="industryColumns.length" class="dialog-section">
              <h3 class="dialog-section-title">行业专项指标</h3>
              <div class="form-grid">
                <template v-for="column in industryColumns" :key="column.prop">
                  <el-form-item :label="column.label" :prop="column.prop">
                    <component :is="resolveEditor(column)" v-model="form[column.prop]" v-bind="resolveEditorProps(column)" />
                  </el-form-item>
                </template>
              </div>
            </section>

            <section class="dialog-section">
              <h3 class="dialog-section-title">评分结果（模型生成）</h3>
              <div class="form-grid">
                <template v-for="column in resultColumns" :key="column.prop">
                  <el-form-item :label="column.label" :prop="column.prop">
                    <component
                      :is="resolveEditor(column)"
                      v-model="form[column.prop]"
                      :disabled="column.prop === 'transitionScore'"
                      v-bind="resolveEditorProps(column)"
                    />
                  </el-form-item>
                </template>
              </div>
            </section>
          </div>
        </el-scrollbar>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useCompanyReviewMarks } from '../composables/useCompanyReviewMarks'
const { isMarked } = useCompanyReviewMarks()
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElLoading, ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import AssessmentReportDialog from '../components/AssessmentReportDialog.vue'
import RadarHoverPreview from '../components/RadarHoverPreview.vue'
import FinancialDataSummary from '../components/FinancialDataSummary.vue'
import { fetchPendingAssessmentReport } from '../api/assessmentReports'
import { getEnvironmentalRatingClass, getTransitionScoreClass } from '../utils/valueTone'
import {
  creditPendingCompanyData,
  createPendingCompanyData,
  deletePendingCompanyData,
  fetchPendingCompanyData,
  fetchPendingCompanyDataOptions,
  scorePendingCompanyData,
  updatePendingCompanyData
} from '../api/pendingCompanyData'
import { fetchModelTrainingSummary } from '../api/modelTraining'
import { fetchFederatedLearningSummary } from '../api/federatedLearning'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const scoringId = ref(null)
const reportDialogVisible = ref(false)
const assessmentReport = ref(null)
const dialogVisible = ref(false)
const editingId = ref(null)
const records = ref([])
const columns = ref([])
const total = ref(0)
const industryOptions = ref([])
const yearOptions = ref([])
const activeIndustryName = ref('待评估企业数据')
const modelSummary = ref({ latestJob: null, models: [] })
const federatedSummary = ref({ latestJob: null, models: [] })
const formRef = ref()
const dialogIndustryKey = ref('power')
const dialogSourceIndustryKey = ref('power')
const dialogColumns = ref([])
let radarShowTimer = null
let radarHideTimer = null

const currentUser = computed(() => {
  const raw = localStorage.getItem('financialUser')
  return raw ? JSON.parse(raw) : null
})

const filters = reactive({
  page: 1,
  size: 10,
  institutionId: '',
  industryKey: 'power',
  companyCode: '',
  stockCode: '',
  dataYear: null
})

const form = reactive({})
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  row: null
})
const radarPreview = reactive({
  visible: false,
  x: 0,
  y: 0,
  row: null
})

const dialogTitle = computed(() => (editingId.value ? '修改待评估企业数据' : '新增待评估企业数据'))
const stockCodePlaceholder = computed(() => (form.companyType === '非上市企业' ? '非上市企业无需填写' : '请输入股票代码'))

const commonProps = new Set(['id', 'industryName', 'companyCode', 'companyName', 'stockCode', 'companyType', 'dataYear'])
const resultProps = new Set(['transitionScore'])
const hiddenProps = new Set(['id', 'institutionCode', 'institutionName', 'companyName', 'stockCode', 'verifiedTransitionPerformance'])

const governanceProps = new Set([
  'transitionTargetCommitment',
  'transitionStrategyPath',
  'governanceIncentive',
  'transitionTechnologyApplication',
  'carbonAssetManagement',
  'riskManagementMarketOpportunity',
  'informationDisclosureTransparency',
  'justTransition',
  'environmentalCompliance',
  'salesNetProfitMargin',
  'revenueGrowthRate',
  'rAndDIntensity'
])

const tableColumns = computed(() => columns.value.filter((column) => !hiddenProps.has(column.prop)))
const editableColumns = computed(() => dialogColumns.value.filter((column) => !commonProps.has(column.prop) && !resultProps.has(column.prop) && column.prop !== 'verifiedTransitionPerformance'))
const governanceColumns = computed(() => editableColumns.value.filter((column) => governanceProps.has(column.prop)))
const industryColumns = computed(() => editableColumns.value.filter((column) => !governanceProps.has(column.prop) && !resultProps.has(column.prop)))
const resultColumns = computed(() => dialogColumns.value.filter((column) => resultProps.has(column.prop)))
const isCurrentIndustryModelReady = computed(() => {
  return (modelSummary.value.models || []).some(
    (item) => item.industryKey === filters.industryKey
      && item.targetName === 'verified_transition_performance'
      && item.status === 'READY'
  )
})
const isCurrentFederatedModelReady = computed(() => {
  return (federatedSummary.value.models || []).some(
    (item) => item.industryKey === filters.industryKey
      && item.targetName === 'verified_transition_performance'
      && item.status === 'READY'
  )
})
const hasAnyScoringModelReady = computed(() => isCurrentIndustryModelReady.value || isCurrentFederatedModelReady.value)

// 概览直接响应列表刷新，评分状态以有效数值判断，空值不计作零分。
const dataSummary = computed(() => {
  const scoredCount = records.value.filter(row => row.transitionScore !== null
    && row.transitionScore !== undefined && String(row.transitionScore).trim() !== ''
    && Number.isFinite(Number(row.transitionScore))).length
  return [
    { label: '匹配待评估记录', value: total.value, unit: '条' },
    { label: '本页已评分', value: scoredCount, unit: '条', tone: 'blue' },
    { label: '本页待评分', value: records.value.length - scoredCount, unit: '条', tone: 'amber' },
    { label: '本页企业', value: new Set(records.value.map(row => row.companyCode).filter(Boolean)).size, unit: '家', tone: 'neutral' }
  ]
})

const rules = computed(() => {
  const base = {
    companyCode: [{ required: true, message: '请输入企业编码', trigger: 'blur' }],
    stockCode: form.companyType === '上市企业' ? [{ required: true, message: '请输入股票代码', trigger: 'blur' }] : [],
    companyType: [{ required: true, message: '请选择企业类型', trigger: 'change' }],
    dataYear: [{ required: true, message: '请选择年份', trigger: 'change' }]
  }
  editableColumns.value.forEach((column) => {
    if (column.type === 'text' && !base[column.prop]) {
      base[column.prop] = []
    }
  })
  return base
})

onMounted(async () => {
  if (!currentUser.value?.institutionId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  filters.institutionId = currentUser.value.institutionId
  // 总览跳转沿用原筛选机制，不另建一套评估操作或数据查询。
  if (typeof route.query.industryKey === 'string') filters.industryKey = route.query.industryKey
  const requestedYear = Number(route.query.dataYear)
  if (Number.isInteger(requestedYear) && requestedYear >= 2000 && requestedYear <= 2100) filters.dataYear = requestedYear
  await loadOptions()
  await loadModelStatus()
  await loadData()
})

onBeforeUnmount(() => {
  clearRadarTimers()
})

function createEmptyForm() {
  const base = {
    companyName: '',
    companyCode: '',
    stockCode: '',
    companyType: '',
    dataYear: new Date().getFullYear()
  }
  editableColumns.value.forEach((column) => {
    base[column.prop] = defaultValueByType(column)
  })
  resultColumns.value.forEach((column) => {
    base[column.prop] = null
  })
  return base
}

function defaultValueByType(column) {
  if (column.type === 'boolean') {
    return 0
  }
  return null
}

function resetForm(nextValues = {}) {
  Object.keys(form).forEach((key) => {
    delete form[key]
  })
  Object.assign(form, createEmptyForm(), nextValues)
}

function fillFormFromRow(row) {
  const next = createEmptyForm()
  Object.keys(next).forEach((key) => {
    if (row[key] !== undefined) {
      next[key] = row[key]
    }
  })
  next.companyName = row.companyName
  next.companyCode = row.companyCode
  next.stockCode = row.stockCode === '-' ? '' : row.stockCode
  next.companyType = normalizeCompanyType(row.companyType)
  next.dataYear = row.dataYear
  Object.assign(form, next)
}

/**
 * 加载待评估页的行业与年份筛选项。
 * <p>这里保留静默失败，避免筛选接口偶发失败时影响主表格渲染。</p>
 */
async function loadOptions() {
  try {
    const result = await fetchPendingCompanyDataOptions({
      institutionId: filters.institutionId,
      industryKey: filters.industryKey
    })
    industryOptions.value = result.industries || []
    yearOptions.value = result.years || []
    if (!industryOptions.value.some((item) => item.key === filters.industryKey) && industryOptions.value.length > 0) {
      filters.industryKey = industryOptions.value[0].key
      await loadOptions()
    }
    return true
  } catch {
    industryOptions.value = []
    yearOptions.value = []
    return false
  }
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchPendingCompanyData(buildQueryParams())
    columns.value = result.columns || []
    records.value = result.page?.records || []
    total.value = Number(result.page?.total || 0)
    activeIndustryName.value = result.industryName || '待评估企业数据'
  } catch {
    // 请求失败时保留当前表格数据，避免刷新失败导致列表闪空。
  } finally {
    loading.value = false
  }
}

async function loadModelSummary() {
  modelSummary.value = await fetchModelTrainingSummary(filters.institutionId)
}

async function loadFederatedSummary() {
  federatedSummary.value = await fetchFederatedLearningSummary()
}

async function loadModelStatus() {
  await Promise.all([loadModelSummary(), loadFederatedSummary()])
}

async function loadDialogColumns(industryKey) {
  try {
    const result = await fetchPendingCompanyData({
      institutionId: filters.institutionId,
      industryKey,
      page: 1,
      size: 1
    })
    dialogColumns.value = result.columns || []
  } catch {
    dialogColumns.value = []
  }
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

async function handleIndustryChange() {
  filters.page = 1
  filters.dataYear = null
  await loadOptions()
  await loadModelStatus()
  await loadData()
}

function handleSearch() {
  filters.page = 1
  loadData()
}

async function handleReset() {
  Object.assign(filters, {
    page: 1,
    size: 10,
    companyCode: '',
    stockCode: '',
    dataYear: null
  })
  await loadOptions()
  await loadData()
}

function handleSizeChange() {
  filters.page = 1
  loadData()
}

function openCreateDialog() {
  editingId.value = null
  dialogIndustryKey.value = filters.industryKey
  dialogSourceIndustryKey.value = filters.industryKey
  dialogColumns.value = [...columns.value]
  resetForm()
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEditDialog(row) {
  closeContextMenu()
  editingId.value = row.id
  dialogSourceIndustryKey.value = filters.industryKey
  dialogIndustryKey.value = filters.industryKey
  dialogColumns.value = [...columns.value]
  fillFormFromRow(row)
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleDialogIndustryChange() {
  const snapshot = { ...form }
  await loadDialogColumns(dialogIndustryKey.value)
  resetForm(snapshot)
  formRef.value?.clearValidate()
}

function handleCompanyTypeChange() {
  if (form.companyType === '非上市企业') {
    form.stockCode = ''
  }
  formRef.value?.clearValidate('stockCode')
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      institutionId: filters.institutionId,
      industryKey: dialogIndustryKey.value,
      sourceIndustryKey: editingId.value ? dialogSourceIndustryKey.value : dialogIndustryKey.value,
      data: buildPayloadData()
    }
    if (editingId.value) {
      await updatePendingCompanyData(editingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createPendingCompanyData(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    filters.industryKey = dialogIndustryKey.value
    filters.page = 1
    filters.dataYear = null
    await loadOptions()
    await loadData()
  } catch {
    // 统一错误提示由请求拦截器处理，这里不继续抛出异常。
  } finally {
    saving.value = false
  }
}

function buildPayloadData() {
  const payload = {
    companyCode: form.companyCode,
    stockCode: form.companyType === '非上市企业' ? '' : form.stockCode,
    companyType: form.companyType,
    dataYear: form.dataYear
  }
  editableColumns.value.forEach((column) => {
    if (column.prop !== 'verifiedTransitionPerformance') {
      payload[column.prop] = form[column.prop]
    }
  })
  resultColumns.value.forEach((column) => {
    payload[column.prop] = form[column.prop]
  })
  return payload
}

async function handleStartScore(row) {
  if (!hasAnyScoringModelReady.value) {
    ElMessage.warning('当前行业的本机构模型和联邦模型均未就绪')
    return
  }
  const modelMode = await chooseScoreModelMode()
  if (!modelMode) {
    return
  }
  scoringId.value = row.id
  try {
    // 先调用模型完成评分，再刷新列表和报告，保证页面看到的是最新结果。
    await scorePendingCompanyData(row.id, {
      institutionId: filters.institutionId,
      industryKey: filters.industryKey,
      modelMode
    })
    ElMessage.success(`${modelMode === 'federated' ? '联邦模型' : '本机构模型'}评分已写入`)
    await loadData()
    await openAssessmentReport(row.id, modelMode)
  } catch {
    // 评分失败时由统一拦截器提示，这里只保证 loading 状态最终被清理。
  } finally {
    scoringId.value = null
  }
}

async function openAssessmentReport(id, modelMode) {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '报告正在生成中，请稍候...',
    background: 'rgba(15, 23, 42, 0.28)'
  })
  try {
    assessmentReport.value = await fetchPendingAssessmentReport(filters.industryKey, filters.institutionId, id, {
      modelMode
    })
    reportDialogVisible.value = true
  } catch {
    // 评分已完成时，报告生成失败由统一 Axios 拦截器提示；这里不重复弹窗。
  } finally {
    loadingInstance.close()
  }
}

async function chooseScoreModelMode() {
  if (isCurrentIndustryModelReady.value && isCurrentFederatedModelReady.value) {
    try {
      await ElMessageBox.confirm(
        '请选择本次评分使用的模型。联邦模型会调用同一行业下各金融机构模型的加权集成结果；本机构模型只使用当前金融机构自己的行业模型。',
        '选择评分模型',
        {
          confirmButtonText: '联邦模型评分',
          cancelButtonText: '本机构模型评分',
          distinguishCancelAndClose: true,
          type: 'info'
        }
      )
      return 'federated'
    } catch (action) {
      return action === 'cancel' ? 'local' : ''
    }
  }
  if (isCurrentFederatedModelReady.value) {
    try {
      await ElMessageBox.confirm('当前行业本机构模型未就绪，将使用联邦模型进行评分。', '联邦模型评分确认', {
        confirmButtonText: '使用联邦模型',
        cancelButtonText: '取消',
        type: 'info'
      })
      return 'federated'
    } catch {
      return ''
    }
  }
  return 'local'
}

async function handleCredit(row) {
  if (!hasTransitionScore(row)) {
    ElMessage.warning('请先点击开始评估生成转型综合评分')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认对 ${row.companyCode || '未关联企业'} 执行授信吗？确认后该条数据会归档到已评估的历史数据，并将经核验转型绩效设为“是”。`,
      '授信确认',
      {
        confirmButtonText: '确认授信',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    // 授信后由后端同时写入历史表并删除待评估记录，前端只负责刷新当前列表。
    await creditPendingCompanyData(row.id, {
      institutionId: filters.institutionId,
      industryKey: filters.industryKey
    })
    ElMessage.success('授信完成，已归档到历史数据')
    await loadData()
  } catch {
    // 请求错误由统一 Axios 拦截器提示，这里避免重复弹窗。
  }
}

async function handleDelete(row) {
  closeContextMenu()
  try {
    await ElMessageBox.confirm(`确认删除 ${row.companyCode || '未关联企业'} ${row.dataYear} 年的待评估数据吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePendingCompanyData(filters.industryKey, filters.institutionId, row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // 用户取消或接口失败都不继续向外抛出，保持控制台干净。
  }
}

function openContextMenu(row, column, event) {
  event.preventDefault()
  contextMenu.row = row
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.visible = true
}

function closeContextMenu() {
  contextMenu.visible = false
}

function resolveEditor(column) {
  if (column.type === 'boolean') {
    return 'el-switch'
  }
  if (column.prop === 'dataYear') {
    return 'el-input-number'
  }
  if (column.type === 'number') {
    return 'el-input-number'
  }
  return 'el-input'
}

function resolveEditorProps(column) {
  if (column.type === 'boolean') {
    return { activeValue: 1, inactiveValue: 0 }
  }
  if (column.prop === 'dataYear') {
    return { controls: false, min: 2000, max: 2100, precision: 0, step: 1 }
  }
  if (column.type === 'number') {
    return { controls: false, precision: 4, step: 0.01 }
  }
  return {}
}

function scheduleRadarPreview(row, event) {
  clearRadarTimers()
  radarPreview.visible = false
  radarPreview.row = row
  radarPreview.x = event.clientX
  radarPreview.y = event.clientY
  radarShowTimer = window.setTimeout(() => {
    radarPreview.visible = true
  }, 1000)
}

function moveRadarPreview(event) {
  if (!radarPreview.visible) {
    radarPreview.x = event.clientX
    radarPreview.y = event.clientY
  }
}

function keepRadarPreview() {
  if (radarHideTimer) {
    window.clearTimeout(radarHideTimer)
    radarHideTimer = null
  }
}

function scheduleHideRadarPreview() {
  if (radarShowTimer) {
    window.clearTimeout(radarShowTimer)
    radarShowTimer = null
  }
  radarHideTimer = window.setTimeout(() => {
    radarPreview.visible = false
  }, 120)
}

function clearRadarTimers() {
  if (radarShowTimer) {
    window.clearTimeout(radarShowTimer)
    radarShowTimer = null
  }
  if (radarHideTimer) {
    window.clearTimeout(radarHideTimer)
    radarHideTimer = null
  }
}

function formatBoolean(value) {
  if (value === 1) {
    return '是'
  }
  if (value === 0) {
    return '否'
  }
  return '-'
}

function formatTableCell(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}

function getCellToneClass(prop, value) {
  if (prop === 'environmentalPerformanceRating') {
    return ['value-tone', getEnvironmentalRatingClass(value)]
  }
  if (prop === 'transitionScore') {
    return ['value-tone', getTransitionScoreClass(value)]
  }
  return ''
}

function hasTransitionScore(row) {
  return row.transitionScore !== null
    && row.transitionScore !== undefined
}

function normalizeCompanyType(value) {
  if (value === '上市公司') {
    return '上市企业'
  }
  if (value === '非上市公司') {
    return '非上市企业'
  }
  return value || ''
}
</script>

<style scoped>
.company-review-mark { color: #d03030 !important; }
.marked-company-name { display: block; color: inherit; font-size: 12px; line-height: 1.5; white-space: normal; overflow-wrap: anywhere; }
.pending-page {
  position: relative;
  min-height: 100%;
  padding: 20px;
}

.filter-strip,
.workspace-band {
  width: 100%;
  background: #fff;
  border: 1px solid #e3e8ef;
  border-radius: 6px;
}

.filter-strip {
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 18px;
  padding: 18px 20px 6px;
}

.filter-title h2,
.workspace-header h2 {
  margin: 0;
  color: #23364f;
  font-size: 17px;
  font-weight: 800;
}

.filter-title span,
.workspace-header p {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr)) auto;
  gap: 8px 12px;
  align-items: start;
}

.filter-form :deep(.el-select) {
  width: 100%;
}

.filter-actions {
  min-width: 164px;
}

.workspace-band {
  margin-top: 16px;
  padding: 16px 20px;
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pending-table :deep(.el-table__row) {
  cursor: context-menu;
}

.data-table {
  width: 100%;
}

.company-code-cell {
  display: inline-flex;
  max-width: 100%;
  color: #2563eb;
  cursor: pointer;
  font-weight: 600;
}

.value-tone {
  font-weight: 700;
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

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  overflow-x: auto;
}

.context-menu {
  position: fixed;
  z-index: 3000;
  width: 132px;
  padding: 6px;
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.18);
}

.context-menu button {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
  padding: 9px 10px;
  color: #334155;
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.context-menu button:hover {
  background: #f1f5f9;
}

.context-menu .danger {
  color: #f56c6c;
}

.data-dialog :deep(.el-input-number),
.data-dialog :deep(.el-select) {
  width: 100%;
}

.dialog-grid {
  display: grid;
  gap: 14px;
}

.dialog-section-title {
  margin: 0 0 12px;
  color: #334155;
  font-size: 15px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(240px, 1fr));
  gap: 14px 16px;
}

@media (max-width: 1300px) {
  .filter-strip {
    grid-template-columns: 1fr;
  }

  .filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 720px) {
  .pending-page {
    padding: 12px;
  }

  .workspace-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-form,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
