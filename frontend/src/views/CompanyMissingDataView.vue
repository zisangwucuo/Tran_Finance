<template>
  <div class="missing-page">
    <section class="top-band">
      <div>
        <h2>待补全数据</h2>
        <p>{{ currentUser?.companyCode || '当前企业' }}</p>
      </div>
      <div class="top-actions">
        <el-tag effect="plain" type="warning">待处理 {{ total }} 条</el-tag>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </section>

    <section class="table-band">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column fixed="left" label="ID" prop="id" width="90" />
        <el-table-column label="金融机构" prop="institutionName" min-width="180" show-overflow-tooltip />
        <el-table-column label="行业" prop="industryName" width="140" />
        <el-table-column label="年份" prop="dataYear" width="100" />
        <el-table-column label="缺失字段" min-width="320">
          <template #default="{ row }">
            <el-tag
              v-for="item in parseColumns(row.missingColumnsJson)"
              :key="item.prop"
              class="column-tag"
              effect="plain"
              type="danger"
            >
              {{ item.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="补全状态" width="130">
          <template #default="{ row }">
            <el-tag :type="row.changedColumnsJson ? 'success' : 'info'" effect="plain">
              {{ row.changedColumnsJson ? '已填补' : '待填补' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提醒时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="170">
          <template #default="{ row }">
            <el-button :icon="Edit" link type="primary" @click="openFillDialog(row)">填补</el-button>
            <el-button :icon="Check" link type="success" @click="handleSubmit(row)">提交</el-button>
          </template>
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

    <el-dialog v-model="fillVisible" :title="`${editingReminder?.companyCode || '未关联企业'} 数据补全`" width="760px">
      <el-alert
        show-icon
        title="请只补全金融机构提醒的缺失字段，提交后将同步更新金融机构历史评估数据。"
        type="info"
      />
      <el-form ref="fillFormRef" :model="fillForm" label-width="180px" class="fill-form">
        <el-form-item
          v-for="column in missingColumns"
          :key="column.prop"
          :label="column.label"
          :prop="column.prop"
          :rules="[{ required: true, message: `请填写${column.label}`, trigger: 'blur' }]"
        >
          <component :is="resolveEditor(column)" v-model="fillForm[column.prop]" v-bind="resolveEditorProps(column)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fillVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveFillValues">保存填补</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Edit, Refresh } from '@element-plus/icons-vue'
import {
  fetchCompanyMissingData,
  fetchCompanyMissingDataDetail,
  saveCompanyMissingValues,
  submitCompanyMissingData
} from '../api/companyMissingData'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const fillVisible = ref(false)
const records = ref([])
const total = ref(0)
const editingReminder = ref(null)
const missingColumns = ref([])
const fillFormRef = ref()
const fillForm = reactive({})

const currentUser = computed(() => {
  const raw = localStorage.getItem('companyUser')
  return raw ? JSON.parse(raw) : null
})

const query = reactive({
  page: 1,
  size: 10
})

onMounted(async () => {
  if (!currentUser.value?.companyCode) {
    ElMessage.warning('请先登录企业系统')
    router.push('/company/login')
    return
  }
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const result = await fetchCompanyMissingData({
      companyCode: currentUser.value.companyCode,
      page: query.page,
      size: query.size
    })
    records.value = result.records || []
    total.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  query.page = 1
  loadData()
}

async function openFillDialog(row) {
  const detail = await fetchCompanyMissingDataDetail(row.id, currentUser.value.companyCode)
  editingReminder.value = row
  missingColumns.value = detail.missingColumns || []
  Object.keys(fillForm).forEach((key) => delete fillForm[key])
  missingColumns.value.forEach((column) => {
    fillForm[column.prop] = defaultValue(column)
  })
  fillVisible.value = true
}

async function saveFillValues() {
  await fillFormRef.value.validate()
  saving.value = true
  try {
    await saveCompanyMissingValues(editingReminder.value.id, {
      companyCode: currentUser.value.companyCode,
      values: { ...fillForm }
    })
    ElMessage.success('缺失字段已保存')
    fillVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleSubmit(row) {
  if (!row.changedColumnsJson) {
    ElMessage.warning('请先点击填补并保存缺失字段')
    return
  }
  await ElMessageBox.confirm('确认提交本条补全数据吗？提交后该提醒将从列表中移除。', '提交确认', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await submitCompanyMissingData(row.id, currentUser.value.companyCode)
  ElMessage.success('已提交给金融机构')
  await loadData()
}

function parseColumns(json) {
  if (!json) return []
  try {
    return typeof json === 'string' ? JSON.parse(json) : json
  } catch {
    return []
  }
}

function resolveEditor(column) {
  if (column.type === 'boolean') return 'el-switch'
  if (column.type === 'number') return 'el-input-number'
  return 'el-input'
}

function resolveEditorProps(column) {
  if (column.type === 'boolean') {
    return { activeValue: 1, inactiveValue: 0 }
  }
  if (column.type === 'number') {
    return { controls: false, precision: 4, step: 0.01 }
  }
  return {}
}

function defaultValue(column) {
  if (column.type === 'boolean') return 0
  return null
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.missing-page {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.top-band,
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

.table-band {
  padding: 16px 18px;
}

.column-tag {
  margin: 3px 6px 3px 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.fill-form {
  margin-top: 18px;
}

.fill-form :deep(.el-input-number),
.fill-form :deep(.el-select) {
  width: 100%;
}

@media (max-width: 720px) {
  .top-band {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
