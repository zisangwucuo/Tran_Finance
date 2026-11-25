<template>
  <section class="common-customers">
    <header><h2>查找共同客户</h2><el-button :icon="Refresh" :loading="checking" @click="refreshStatus">刷新节点状态</el-button></header>
    <div class="summary"><el-tag v-for="node in nodes" :key="node.institutionId" :type="node.status === 'READY' ? 'success' : 'info'">{{ node.institutionId }}：{{ node.status === 'READY' ? '就绪' : '未就绪' }}</el-tag></div>
    <div class="filters">
      <el-select v-model="industry" :disabled="loading" aria-label="行业"><el-option v-for="i in industries" :key="i.key" :label="i.name" :value="i.key" /></el-select>
      <el-radio-group v-model="dataset" :disabled="loading"><el-radio-button value="history">历史数据</el-radio-button><el-radio-button value="pending">待评估数据</el-radio-button></el-radio-group>
      <el-button type="primary" :loading="loading" :disabled="!ready" @click="load">开始隐私求交</el-button>
      <el-select v-model="year" clearable placeholder="全部年份" aria-label="年份"><el-option v-for="y in years" :key="y" :label="String(y)" :value="y" /></el-select>
      <el-input v-model="keyword" clearable placeholder="请输入企业编码" :prefix-icon="Search" />
      <el-select v-model="bank" clearable placeholder="其他金融机构"><el-option v-for="b in banks" :key="b.code" :label="b.name" :value="b.code" /></el-select>
      <el-checkbox v-model="onlyDifferent">仅看存在差异</el-checkbox>
    </div>
    <div class="summary"><span>共同客户 <strong>{{ customerCount }}</strong> 家</span><span>跨机构记录对 <strong>{{ scopedRows.length }}</strong> 组</span><span>存在差异 <strong class="warning">{{ conflictCount }}</strong> 组</span><span>指标一致 <strong>{{ consistentCount }}</strong> 组</span><span>重复待核查 <strong>{{ ambiguousCount }}</strong> 组</span></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table v-loading="loading" :data="paged" stripe border empty-text="当前条件下没有共同客户记录">
      <el-table-column prop="companyCode" label="企业编码" min-width="130" show-overflow-tooltip />
      
      <el-table-column prop="year" label="年份" width="85" />
      <el-table-column prop="otherInstitutionName" label="对方金融机构" min-width="140" show-overflow-tooltip />
      <el-table-column label="核验结果" min-width="150"><template #default="{ row }"><el-tag :type="row.status === 'CONSISTENT' ? 'success' : 'warning'">{{ { CONSISTENT: '指标一致', DIFFERENT: '指标不一致', AMBIGUOUS: '重复记录待核查' }[row.status] || '未知状态' }}</el-tag></template></el-table-column>
      <el-table-column label="差异指标" width="135" align="center"><template #default="{ row }"><el-button type="primary" link @click="detailRow = row; detailVisible = true">查看详情</el-button></template></el-table-column>
    </el-table>
    <div class="pagination-row"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]" :total="filtered.length" background layout="total, prev, pager, next, sizes, jumper" /></div>
    <el-alert v-if="!ready" title="三个银行节点尚未全部就绪" type="info" :closable="false" />
    <el-alert v-if="completed" :title="`核验完成：本行 ${localCount} 组企业年度数据，跳过无法匹配企业编码的记录 ${skipped} 条`" type="info" :closable="false" />
    <el-dialog v-model="detailVisible" title="差异指标详情" width="620px" style="max-width: calc(100vw - 32px)" :close-on-click-modal="!marking">
      <template v-if="detailRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="企业编码">{{ detailRow.companyCode }}</el-descriptions-item>
          
          <el-descriptions-item label="年份">{{ detailRow.year }}</el-descriptions-item>
          <el-descriptions-item label="对方金融机构">{{ detailRow.otherInstitutionName }}</el-descriptions-item>
        </el-descriptions>
        <el-table v-if="detailRow.status === 'DIFFERENT'" :data="(detailRow.differentFieldLabels || []).map(label => ({ label }))" max-height="360" stripe class="detail-indicators">
          <el-table-column type="index" label="序号" width="65" />
          <el-table-column prop="label" label="取值不一致的指标" />
        </el-table>
        <el-empty v-else :description="detailRow.status === 'AMBIGUOUS' ? '重复记录存在冲突，暂不能定位差异指标' : '各项指标取值一致'" :image-size="70" />
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="danger" :icon="Flag" :loading="marking" :disabled="!marksReady || detailRow?.status !== 'DIFFERENT' || isMarked(detailRow)" @click="handleMark">{{ isMarked(detailRow) ? '已标记' : '标记' }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Search, Refresh, Flag } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCompanyReviewMarks } from '../composables/useCompanyReviewMarks'
import request from '../api/request'

// 按行业读取只读核查结果，筛选与分页不改变任何银行的原始数据。
const industries = ref([]), industry = ref('power'), dataset = ref('history')
const rows = ref([]), loading = ref(false), error = ref('')
const nodes = ref([]), checking = ref(false), completed = ref(false), skipped = ref(0), localCount = ref(0)
const ready = computed(() => nodes.value.length === 3 && nodes.value.every(n => n.status === 'READY'))
const year = ref(''), bank = ref(''), keyword = ref(''), onlyDifferent = ref(true), page = ref(1)
const pageSize = ref(10)
const { isMarked, markCompany, ready: marksReady } = useCompanyReviewMarks()
const detailVisible = ref(false), detailRow = ref(null), marking = ref(false)
async function handleMark() {
  if (!detailRow.value || marking.value) return
  marking.value = true
  try { await markCompany(detailRow.value); ElMessage.success('已标记，该企业将在历史数据和待评估数据中显示为红色') }
  catch { /* 请求模块统一显示失败原因；失败时保持未标记状态。 */ }
  finally { marking.value = false }
}
let sequence = 0
const years = computed(() => [...new Set(rows.value.map(r => r.year))].sort((a, b) => b - a))
const banks = computed(() => [...new Map(rows.value.map(r => [r.otherInstitution, { code: r.otherInstitution, name: r.otherInstitutionName }])).values()])
// 汇总跟随年份、机构、企业条件；差异开关仅缩小表格，保留可比较的状态统计。
const scopedRows = computed(() => rows.value.filter(r => (!year.value || r.year === year.value)
  && (!bank.value || r.otherInstitution === bank.value)
  && `${r.companyCode || ''}`.toLowerCase().includes(keyword.value.trim().toLowerCase())))
const customerCount = computed(() => new Set(scopedRows.value.map(r => r.companyCode)).size)
const conflictCount = computed(() => scopedRows.value.filter(r => r.status === 'DIFFERENT').length)
const consistentCount = computed(() => scopedRows.value.filter(r => r.status === 'CONSISTENT').length)
const ambiguousCount = computed(() => scopedRows.value.filter(r => r.status === 'AMBIGUOUS').length)
const filtered = computed(() => scopedRows.value.filter(r => !onlyDifferent.value || r.status === 'DIFFERENT'))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
watch([year, bank, keyword, onlyDifferent, pageSize], () => { page.value = 1 })
// 切换数据范围后清空旧结果；只有明确点击按钮才执行加密求交。
watch([industry, dataset], () => { ++sequence; rows.value = []; completed.value = false; year.value = ''; bank.value = ''; page.value = 1 })
async function refreshStatus() {
  checking.value = true
  try { nodes.value = await request.get('/common-customers/status', { timeout: 15000 }) }
  catch (e) { nodes.value = []; error.value = e.response?.data?.message || e.message }
  finally { checking.value = false }
}
async function load() {
  // 快速切换行业时只接收最后一次请求，避免旧响应覆盖新页面。
  const current = ++sequence
  loading.value = true; error.value = ''; rows.value = []; completed.value = false
  year.value = ''; bank.value = ''; page.value = 1
  try {
    const data = await request.post('/common-customers', { industryKey: industry.value, dataset: dataset.value }, { timeout: 210000 })
    if (current === sequence) { rows.value = data.records; skipped.value = data.skippedLocalRows; localCount.value = data.localCustomerYears; completed.value = true }
  } catch (e) { if (current === sequence) error.value = e.response?.data?.message || e.message || '隐私求交失败' }
  finally { if (current === sequence) loading.value = false }
}
onMounted(async () => {
  try { industries.value = await request.get('/common-customers/industries'); await refreshStatus() }
  catch (e) { error.value = e.response?.data?.message || e.message }
})
</script>

<style scoped>
.common-customers { background: white; padding: 22px; min-width: 0; }
header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
h2 { font-size: 20px; margin: 0; }
.filters { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 24px 0 18px; }
.filters .el-select { width: 165px; }.filters .el-input { width: 220px; }
.summary { display: flex; flex-wrap: wrap; gap: 28px; padding: 14px 0 20px; color: #526777; }
.summary strong { color: #176b9b; font-size: 21px; margin-left: 8px; }
.warning, .summary .warning { color: #b76b08; }.record-ids { color: #687885; }
.pagination-row { display: flex; justify-content: flex-end; margin-top: 12px; overflow-x: auto; }
.detail-indicators { margin-top: 18px; }
</style>
