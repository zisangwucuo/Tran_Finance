<template>
  <div class="notice-page">
    <section class="top-band">
      <div>
        <h2>数据补全协同</h2>
        <p>{{ currentUser?.institutionName || currentUser?.institutionId || '当前金融机构' }}</p>
      </div>
      <div class="top-actions">
        <el-tag effect="plain" type="success">共 {{ total }} 条</el-tag>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </section>

    <section class="filter-band">
      <el-form :model="query" class="filter-form" label-position="top" @submit.prevent>
        <el-form-item label="企业编码">
          <el-input
            v-model.trim="query.keyword"
            clearable
            placeholder="请输入企业编码"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="query.industryKey" clearable placeholder="全部行业">
            <el-option v-for="item in industryOptions" :key="item.key" :label="item.name" :value="item.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="query.dataYear" clearable placeholder="全部年份">
            <el-option v-for="year in yearOptions" :key="year" :label="year" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="待填补" value="PENDING" />
            <el-option label="已提交" value="SUBMITTED" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
          <el-button :icon="Filter" plain type="success" @click="filterSubmitted">已提交</el-button>
        </el-form-item>
      </el-form>
    </section>

    <FinancialDataSummary :items="dataSummary" :loading="loading" />

    <section class="table-band">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column fixed="left" label="ID" prop="id" width="65" />
        <el-table-column label="企业编码" prop="companyCode" width="140" />
        <el-table-column label="行业" prop="industryName" width="115" />
        <el-table-column label="年份" prop="dataYear" width="80" />
        <el-table-column label="数据行ID" prop="sourceRowId" width="95" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUBMITTED' ? 'success' : 'warning'" effect="plain">
              {{ row.status === 'SUBMITTED' ? '已提交' : '待填补' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="补全字段" min-width="260">
          <template #default="{ row }">
            <div v-if="row.status === 'SUBMITTED'">
              <div v-for="item in parseJson(row.changedColumnsJson)" :key="item.prop" class="change-line">
                <strong>{{ item.label }}</strong>
                <span>{{ formatValue(item.oldValue) }} → {{ formatValue(item.newValue) }}</span>
              </div>
            </div>
            <div v-else>
              <el-tag
                v-for="item in parseJson(row.missingColumnsJson)"
                :key="item.prop"
                class="field-tag"
                effect="plain"
                type="danger"
              >
                {{ item.label }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDate(row.submittedAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, prev, pager, next, sizes, jumper"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Filter, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { fetchFinancialDataNotices } from '../api/financialNotices'
import { industryOptions } from '../utils/industryOptions'
import FinancialDataSummary from '../components/FinancialDataSummary.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
// 通知总数按当前筛选口径返回，状态分布明确限定为本页。
const dataSummary = computed(() => [
  { label: '匹配通知', value: total.value, unit: '条' },
  { label: '本页待填补', value: records.value.filter(row => row.status === 'PENDING').length, unit: '条', tone: 'amber' },
  { label: '本页已提交', value: records.value.filter(row => row.status === 'SUBMITTED').length, unit: '条', tone: 'blue' },
  { label: '本页企业', value: new Set(records.value.map(row => row.companyCode).filter(Boolean)).size, unit: '家', tone: 'neutral' }
])
const yearOptions = ref([])
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  industryKey: '',
  dataYear: null,
  status: ''
})

const currentUser = computed(() => {
  const raw = localStorage.getItem('financialUser')
  return raw ? JSON.parse(raw) : null
})

onMounted(async () => {
  if (!currentUser.value?.institutionId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // 从客户总览进入时直接定位当前年份的待提交通知。
  if (['PENDING', 'SUBMITTED'].includes(route.query.status)) query.status = route.query.status
  const requestedYear = Number(route.query.dataYear)
  if (Number.isInteger(requestedYear) && requestedYear >= 2000 && requestedYear <= 2100) query.dataYear = requestedYear
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const result = await fetchFinancialDataNotices({
      institutionId: currentUser.value.institutionId,
      ...buildQueryParams()
    })
    records.value = result.records || []
    total.value = Number(result.total || 0)
    refreshYearOptions(records.value)
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  query.page = 1
  loadData()
}

async function handleSearch() {
  query.page = 1
  await loadData()
}

async function filterSubmitted() {
  query.status = 'SUBMITTED'
  query.page = 1
  await loadData()
}

async function handleReset() {
  Object.assign(query, {
    page: 1,
    size: 10,
    keyword: '',
    industryKey: '',
    dataYear: null,
    status: ''
  })
  await loadData()
}

function buildQueryParams() {
  const params = { ...query }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) {
      delete params[key]
    }
  })
  return params
}

function refreshYearOptions(rows) {
  const years = new Set(yearOptions.value)
  rows.forEach((row) => {
    if (row.dataYear !== null && row.dataYear !== undefined) {
      years.add(row.dataYear)
    }
  })
  if (query.dataYear !== null && query.dataYear !== undefined) {
    years.add(query.dataYear)
  }
  yearOptions.value = Array.from(years).sort((left, right) => Number(right) - Number(left))
}

function parseJson(json) {
  if (!json) return []
  try {
    return typeof json === 'string' ? JSON.parse(json) : json
  } catch {
    return []
  }
}

function formatValue(value) {
  return value === null || value === undefined || value === '' ? '空' : value
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.notice-page {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.top-band,
.filter-band,
.table-band {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.top-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.top-band h2 {
  margin: 0;
  color: #23364f;
  font-size: 17px;
  font-weight: 800;
}

.top-band p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-band {
  padding: 16px 20px 2px;
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
  min-width: 250px;
}

.table-band {
  padding: 16px 18px;
}

.change-line {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  line-height: 1.7;
}

.change-line strong {
  overflow-wrap: anywhere;
  color: #334155;
}

.change-line span {
  color: #475569;
}

.field-tag {
  margin: 3px 6px 3px 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 720px) {
  .top-band {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .filter-form {
    grid-template-columns: 1fr;
  }
}
</style>
