<template>
  <div class="page-band">
    <section class="query-band">
      <div class="section-heading">
        <h2>历史评估档案</h2>
        <span>{{ currentUser?.institutionName || currentUser?.institutionId || '当前金融机构' }}</span>
      </div>

      <el-form :model="filters" class="query-form" label-position="top">
        <el-form-item label="行业">
          <el-select v-model="filters.industryKey" placeholder="请选择行业" @change="handleIndustryChange">
            <el-option
              v-for="industry in industryOptions"
              :key="industry.key"
              :label="industry.name"
              :value="industry.key"
            />
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
        <el-form-item label="经核验转型绩效">
          <el-select v-model="filters.verifiedTransitionPerformance" clearable placeholder="请选择">
            <el-option :value="1" label="是" />
            <el-option :value="0" label="否" />
          </el-select>
        </el-form-item>
        <el-form-item class="query-actions">
          <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <FinancialDataSummary :items="dataSummary" :loading="loading" />

    <section class="table-band">
      <div class="table-header">
        <div>
          <h2>{{ activeIndustryName }} · 历史记录</h2>
        </div>
        <div class="header-actions">
          <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
          <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" border class="data-table">
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
              @mouseenter="scheduleTrendPreview(row, $event)"
              @mousemove="moveTrendPreview($event)"
              @mouseleave="scheduleHideTrendPreview"
            >
              {{ formatTableCell(row.companyCode) }}
            </span>
            <span v-else :class="[getCellToneClass(column.prop, row[column.prop]), { 'company-review-mark': column.prop === 'companyName' && isMarked(row) }]">
              {{ formatTableCell(row[column.prop]) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="150">
          <template #default="{ row }">
            <el-button :icon="Edit" link type="primary" @click="openEditDialog(row)">修改</el-button>
            <el-button :icon="Delete" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <TransitionScoreTrendHoverPreview
      :company-code="trendPreview.row?.companyCode || ''"
      :industry-key="filters.industryKey"
      :institution-id="filters.institutionId"
      :visible="trendPreview.visible"
      :x="trendPreview.x"
      :y="trendPreview.y"
      @mouseenter="keepTrendPreview"
      @mouseleave="scheduleHideTrendPreview"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1120px" class="data-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-scrollbar max-height="68vh">
          <div class="dialog-grid">
            <section class="dialog-section">
              <h3 class="dialog-section-title">基础信息</h3>
              <div class="form-grid">
                <el-form-item label="行业类型" prop="industryKey">
                  <el-select
                    v-model="dialogIndustryKey"
                    placeholder="请选择行业类型"
                    @change="handleDialogIndustryChange"
                  >
                    <el-option
                      v-for="industry in industryOptions"
                      :key="industry.key"
                      :label="industry.name"
                      :value="industry.key"
                    />
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
                    <component
                      :is="resolveEditor(column)"
                      v-model="form[column.prop]"
                      v-bind="resolveEditorProps(column)"
                    />
                  </el-form-item>
                </template>
              </div>
            </section>

            <section v-if="industryColumns.length" class="dialog-section">
              <h3 class="dialog-section-title">行业特征</h3>
              <div class="form-grid">
                <template v-for="column in industryColumns" :key="column.prop">
                  <el-form-item :label="column.label" :prop="column.prop">
                    <component
                      :is="resolveEditor(column)"
                      v-model="form[column.prop]"
                      v-bind="resolveEditorProps(column)"
                    />
                  </el-form-item>
                </template>
              </div>
            </section>

            <section class="dialog-section">
              <h3 class="dialog-section-title">结果指标</h3>
              <div class="form-grid">
                <template v-for="column in resultColumns" :key="column.prop">
                  <el-form-item :label="column.label" :prop="column.prop">
                    <component
                      :is="resolveEditor(column)"
                      v-model="form[column.prop]"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import TransitionScoreTrendHoverPreview from '../components/TransitionScoreTrendHoverPreview.vue'
import FinancialDataSummary from '../components/FinancialDataSummary.vue'
import { getEnvironmentalRatingClass, getTransitionScoreClass } from '../utils/valueTone'
import {
  createElectricCompanyData,
  deleteElectricCompanyData,
  fetchElectricCompanyData,
  fetchElectricCompanyDataOptions,
  updateElectricCompanyData
} from '../api/electricCompanyData'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const records = ref([])
const columns = ref([])
const total = ref(0)
const industryOptions = ref([])
const yearOptions = ref([])
const activeIndustryName = ref('已评估的历史数据')
// 总数来自分页接口，其余数值仅统计当前页，避免把分页样本当作全部档案。
const dataSummary = computed(() => {
  const scores = records.value
    .map(row => row.transitionScore)
    .filter(value => value !== null && value !== undefined && String(value).trim() !== '' && Number.isFinite(Number(value)))
    .map(Number)
  return [
    { label: '匹配档案', value: total.value, unit: '条' },
    { label: '本页企业', value: new Set(records.value.map(row => row.companyCode).filter(Boolean)).size, unit: '家', tone: 'blue' },
    { label: '本页已评分', value: scores.length, unit: '条', tone: 'neutral' },
    { label: '本页平均评分', value: scores.length ? (scores.reduce((sum, score) => sum + score, 0) / scores.length).toFixed(1) : '-', unit: scores.length ? '分' : '', tone: 'amber' }
  ]
})
const formRef = ref()
const dialogIndustryKey = ref('power')
const dialogSourceIndustryKey = ref('power')
const dialogColumns = ref([])
let trendShowTimer = null
let trendHideTimer = null

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
  dataYear: null,
  verifiedTransitionPerformance: null
})

const form = reactive({})
const trendPreview = reactive({
  visible: false,
  x: 0,
  y: 0,
  row: null
})

const dialogTitle = computed(() => (editingId.value ? '修改企业评估数据' : '新增企业评估数据'))
const stockCodePlaceholder = computed(() => (form.companyType === '非上市企业' ? '非上市企业无需填写' : '请输入股票代码'))

const commonProps = new Set(['id', 'industryName', 'companyCode', 'companyName', 'stockCode', 'companyType', 'dataYear'])
const resultProps = new Set(['transitionScore', 'verifiedTransitionPerformance'])

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

const tableColumns = computed(() => columns.value.filter((column) => {
  return !['id', 'institutionCode', 'institutionName', 'companyName', 'stockCode'].includes(column.prop)
}))

onMounted(async () => {
  if (!currentUser.value?.institutionId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  filters.institutionId = currentUser.value.institutionId
  // 客户总览进入明细时保留行业及年份；后端仍负责校验行业和机构归属。
  if (typeof route.query.industryKey === 'string') filters.industryKey = route.query.industryKey
  const requestedYear = Number(route.query.dataYear)
  if (Number.isInteger(requestedYear) && requestedYear >= 2000 && requestedYear <= 2100) filters.dataYear = requestedYear
  await loadOptions()
  await loadData()
})

onBeforeUnmount(() => {
  clearTrendTimers()
})

const editableColumns = computed(() => dialogColumns.value.filter((column) => !commonProps.has(column.prop) && !resultProps.has(column.prop)))
const governanceColumns = computed(() => editableColumns.value.filter((column) => governanceProps.has(column.prop)))
const industryColumns = computed(() => editableColumns.value.filter((column) => !governanceProps.has(column.prop)))
const resultColumns = computed(() => dialogColumns.value.filter((column) => resultProps.has(column.prop)))

function createEmptyForm() {
  const base = {
    companyName: '',
    companyCode: '',
    stockCode: '',
    companyType: '',
    dataYear: new Date().getFullYear(),
    verifiedTransitionPerformance: null
  }
  editableColumns.value.forEach((column) => {
    base[column.prop] = defaultValueByType(column)
  })
  resultColumns.value.forEach((column) => {
    base[column.prop] = defaultValueByType(column)
  })
  base.verifiedTransitionPerformance = null
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
  next.stockCode = row.stockCode
  next.companyType = normalizeCompanyType(row.companyType)
  next.dataYear = row.dataYear
  Object.assign(form, next)
}

/**
 * 加载当前金融机构、当前行业可用的筛选项。
 * <p>这里失败时不抛出异常，避免页面首次加载或切换筛选条件时因为网络抖动直接白屏。</p>
 */
async function loadOptions() {
  try {
    const result = await fetchElectricCompanyDataOptions({
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
    const result = await fetchElectricCompanyData(buildQueryParams())
    columns.value = result.columns || []
    records.value = result.page?.records || []
    total.value = Number(result.page?.total || 0)
    activeIndustryName.value = result.industryName || '企业评估数据'
  } catch {
    // 请求失败时保留当前表格内容，避免列表闪空。
  } finally {
    loading.value = false
  }
}

async function loadDialogColumns(industryKey) {
  try {
    const result = await fetchElectricCompanyData({
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
    dataYear: null,
    verifiedTransitionPerformance: null
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
    // 新增/修改使用同一套表单字段，差别只在于是否带有编辑中的行 ID。
    const payload = {
      institutionId: filters.institutionId,
      industryKey: dialogIndustryKey.value,
      sourceIndustryKey: editingId.value ? dialogSourceIndustryKey.value : dialogIndustryKey.value,
      data: buildPayloadData()
    }
    if (editingId.value) {
      await updateElectricCompanyData(editingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createElectricCompanyData(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    filters.industryKey = dialogIndustryKey.value
    filters.page = 1
    filters.dataYear = null
    await loadOptions()
    await loadData()
  } catch {
    // 统一错误提示由 Axios 拦截器处理，这里只负责停止提交流程。
  } finally {
    saving.value = false
  }
}

function buildPayloadData() {
  const payload = {
    companyCode: form.companyCode,
    stockCode: form.companyType === '非上市企业' ? '' : form.stockCode,
    companyType: form.companyType,
    dataYear: form.dataYear,
    verifiedTransitionPerformance: form.verifiedTransitionPerformance
  }
  editableColumns.value.forEach((column) => {
    payload[column.prop] = form[column.prop]
  })
  resultColumns.value.forEach((column) => {
    payload[column.prop] = form[column.prop]
  })
  return payload
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.companyCode || '未关联企业'} ${row.dataYear} 年的数据吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 单条删除只按行 ID + 当前金融机构 + 行业执行，避免误删其他机构数据。
    await deleteElectricCompanyData(filters.industryKey, filters.institutionId, row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // 用户取消或接口失败都不需要继续向外抛出，避免控制台出现未处理 promise。
  }
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

function scheduleTrendPreview(row, event) {
  clearTrendTimers()
  trendPreview.visible = false
  trendPreview.row = row
  trendPreview.x = event.clientX
  trendPreview.y = event.clientY
  trendShowTimer = window.setTimeout(() => {
    trendPreview.visible = true
  }, 1000)
}

function moveTrendPreview(event) {
  if (!trendPreview.visible) {
    trendPreview.x = event.clientX
    trendPreview.y = event.clientY
  }
}

function keepTrendPreview() {
  if (trendHideTimer) {
    window.clearTimeout(trendHideTimer)
    trendHideTimer = null
  }
}

function scheduleHideTrendPreview() {
  if (trendShowTimer) {
    window.clearTimeout(trendShowTimer)
    trendShowTimer = null
  }
  trendHideTimer = window.setTimeout(() => {
    trendPreview.visible = false
  }, 120)
}

function clearTrendTimers() {
  if (trendShowTimer) {
    window.clearTimeout(trendShowTimer)
    trendShowTimer = null
  }
  if (trendHideTimer) {
    window.clearTimeout(trendHideTimer)
    trendHideTimer = null
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

function normalizeCompanyType(value) {
  if (value === '上市公司') {
    return '上市企业'
  }
  if (value === '非上市公司') {
    return '非上市企业'
  }
  return value || ''
}

const rules = computed(() => {
  const base = {
    companyCode: [{ required: true, message: '请输入企业编码', trigger: 'blur' }],
    stockCode: form.companyType === '上市企业' ? [{ required: true, message: '请输入股票代码', trigger: 'blur' }] : [],
    companyType: [{ required: true, message: '请选择企业类型', trigger: 'change' }],
    dataYear: [{ required: true, message: '请选择年份', trigger: 'change' }],
    verifiedTransitionPerformance: [{ required: true, message: '请选择经核验转型绩效结果', trigger: 'change' }]
  }
  editableColumns.value.forEach((column) => {
    if (column.type === 'text' && !base[column.prop]) {
      base[column.prop] = []
    }
  })
  return base
})
</script>

<style scoped>
.company-review-mark { color: #d03030 !important; }
.marked-company-name { display: block; color: inherit; font-size: 12px; line-height: 1.5; white-space: normal; overflow-wrap: anywhere; }
.query-band,
.table-band {
  width: 100%;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.query-band {
  padding: 18px 20px 6px;
}

.table-band {
  margin-top: 14px;
  padding: 16px 20px 16px;
}

.section-heading,
.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-heading h2,
.table-header h2 {
  margin: 0;
  color: #334155;
  font-size: 16px;
  font-weight: 700;
}

.section-heading span,
.table-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.query-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr)) auto;
  gap: 8px 14px;
  margin-top: 18px;
  align-items: start;
}

.query-form :deep(.el-select) {
  width: 100%;
}

.query-actions {
  min-width: 164px;
}

.table-header {
  margin-bottom: 10px;
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

@media (max-width: 1200px) {
  .query-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 720px) {
  .section-heading,
  .table-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .query-form,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
