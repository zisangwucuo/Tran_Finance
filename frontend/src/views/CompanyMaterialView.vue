<template>
  <section class="materials">
    <header><div><h2>材料上传与指标提取</h2><span>{{ template.companyCode || '企业材料' }}</span></div><el-button :icon="Refresh" :loading="refreshing" @click="refresh">刷新</el-button></header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert v-if="template.indicators && !template.aiConfigured" title="AI服务未配置，暂不能提取材料" type="warning" :closable="false" />
    <el-form class="upload-bar" inline label-position="top">
      <el-form-item label="报告数据年份"><el-date-picker v-model="year" type="year" value-format="YYYY" placeholder="选择报告年份" style="width: 155px" /></el-form-item>
      <el-form-item label="评价行业"><el-input :model-value="template.industryName || template.industryKey" disabled style="width: 160px" /></el-form-item>
      <el-form-item label="企业PDF材料"><el-upload ref="uploadRef" :auto-upload="false" accept=".pdf" :limit="1" :on-change="chooseFile" :on-remove="() => file = null" :on-exceed="() => ElMessage.warning('请先移除已选择文件')"><el-button :icon="Upload">选择PDF</el-button></el-upload></el-form-item>
      <el-form-item label=" "><el-button type="primary" :icon="DocumentChecked" :loading="uploading" :disabled="!file || !year || !consent || !template.aiConfigured || processing" @click="start">开始提取</el-button></el-form-item>
    </el-form>
    <el-checkbox v-model="consent">同意将相关材料片段发送至已配置的AI服务进行指标提取</el-checkbox>
    <div class="toolbar"><h3>提取任务</h3><el-button text :icon="Setting" :disabled="!template.indicators" @click="templateVisible = true">查看评价模板</el-button></div>
    <el-table :data="tasks" stripe border max-height="260" highlight-current-row @row-click="selectTask" empty-text="暂无提取任务">
      <el-table-column prop="fileName" label="材料名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="dataYear" label="年份" width="85" />
      <el-table-column label="任务状态" width="160"><template #default="{ row }"><el-tag :type="row.status === 'FAILED' ? 'danger' : row.status === 'COMPLETED' ? (hasCandidates(row) ? 'success' : 'warning') : 'primary'">{{ row.status === 'COMPLETED' && !hasCandidates(row) ? '未提取到候选值' : taskNames[row.status] || row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click.stop="selectTask(row)">查看结果</el-button></template></el-table-column>
    </el-table>
    <template v-if="current">
      <div class="toolbar"><h3>{{ current.fileName }}</h3><el-tag>{{ current.dataYear }}年</el-tag></div>
      <el-progress v-if="processing" :percentage="current.progress" :stroke-width="10" />
      <el-alert :title="current.status === 'COMPLETED' && !hasCandidates(current) ? '本次没有可展示的提取事实，请查看提取诊断；模板待补项不代表报告中的实际取值。' : current.message || '等待提取'" :type="current.status === 'FAILED' ? 'error' : current.status === 'COMPLETED' && !hasCandidates(current) ? 'warning' : 'info'" :closable="false" />
      <template v-if="current.result">
        <div class="toolbar">
          <el-radio-group v-model="resultMode" @change="resultTable?.clearSelection()"><el-radio-button value="facts">已提取指标</el-radio-button><el-radio-button value="missing">模板待补项</el-radio-button></el-radio-group>
          <el-button :icon="Setting" text @click="diagnosticVisible = true">提取诊断</el-button>
        </div>
        <div class="stats"><span>材料 {{ current.result.pageCount }} 页</span><span>检索送审 {{ current.result.searchedPages.length }} 页</span><span>需OCR检查 {{ current.result.ocrPages.length }} 页</span><span>模板 {{ current.result.templateVersion }}</span></div>
        <el-table ref="resultTable" :data="visibleCandidates" border stripe empty-text="本次没有相应结果，可查看提取诊断" @selection-change="rows => selected = rows">
          <el-table-column type="selection" width="45" :selectable="row => current.status === 'COMPLETED' && ['READY', 'REVIEW'].includes(row.status)" />
          <el-table-column prop="label" label="指标" min-width="180" />
          <el-table-column label="候选值" width="90"><template #default="{ row }">{{ row.rawValue || '—' }}</template></el-table-column>
          <el-table-column prop="unit" label="原始单位" min-width="135" />
          <el-table-column label="年份" width="85"><template #default="{ row }">{{ row.year || '—' }}</template></el-table-column>
          <el-table-column label="核验状态" width="120"><template #default="{ row }"><el-tag :type="statusTypes[row.status]">{{ statusNames[row.status] }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><el-button type="primary" link @click="showEvidence(row)">查看依据</el-button></template></el-table-column>
        </el-table>
        <el-button v-if="hasCandidates(current)" class="write-entry" :disabled="!selected.length" @click="writeVisible = !writeVisible">写入待评估数据（可选）</el-button>
        <div v-if="writeVisible && selected.length" class="adoption-bar">
          <el-select v-model="targetId" placeholder="选择本企业待评估记录" style="width: 340px; max-width: 100%"><el-option v-for="row in targets" :key="row.id" :value="row.id" :disabled="adopted.includes(row.id)" :label="`${row.institution_name || row.institution_code} · ${row.data_year}年 · ID ${row.id}${adopted.includes(row.id) ? '（已采用）' : ''}`" /></el-select>
          <el-button type="primary" :icon="Check" :disabled="!targetId || !selected.length" @click="openAdoption">确认采用（{{ selected.length }}项）</el-button>
        </div>
        <el-empty v-if="writeVisible && !targets.length" description="该年份暂无可写入记录；提取结果已保存，无需选择记录" :image-size="65" />
      </template>
    </template>
    <el-dialog v-model="evidenceVisible" title="指标原文与口径核验" width="1100px" style="max-width: calc(100vw - 32px)">
      <div v-if="evidence" class="evidence-layout">
        <div><el-descriptions :column="1" border>
          <el-descriptions-item label="指标">{{ evidence.label }}</el-descriptions-item>
          <el-descriptions-item label="企业编码">{{ template.companyCode || '未关联企业' }}</el-descriptions-item>
          <el-descriptions-item label="核算边界">{{ evidence.boundary || '未明确' }}</el-descriptions-item>
          <el-descriptions-item label="分母">{{ evidence.denominator || '未明确' }}</el-descriptions-item>
          <el-descriptions-item label="PDF页码">{{ evidence.page || '未找到' }}</el-descriptions-item>
        </el-descriptions><blockquote>{{ evidence.quote || '无可核验原文' }}</blockquote><ul><li v-for="reason in evidence.reasons" :key="reason">{{ reason }}</li></ul></div>
        <iframe v-if="pdfUrl" :src="`${pdfUrl}#page=${evidence.page || 1}`" title="PDF原文" />
        <el-empty v-else :description="pdfLoading ? '正在加载原文' : '原文暂不可用'" />
      </div>
    </el-dialog>
    <el-dialog v-model="templateVisible" title="行业评价模板 · 演示口径v1" width="1000px" style="max-width: calc(100vw - 32px)">
      <el-table :data="template.indicators" border><el-table-column prop="label" label="指标" min-width="160" /><el-table-column prop="unit" label="标准单位" min-width="130" /><el-table-column prop="numerator" label="分子" min-width="160" /><el-table-column prop="denominator" label="分母" min-width="140" /><el-table-column prop="source" label="口径出处" min-width="190" /></el-table>
    </el-dialog>
    <el-dialog v-model="diagnosticVisible" title="提取诊断" width="900px" style="max-width: calc(100vw - 32px)">
      <template v-if="current?.result">
        <el-alert :title="current.result.diagnostic || '旧版任务未保留AI结构化响应，请重新上传提取'" type="info" :closable="false" />
        <p>已送审PDF页码：{{ current.result.searchedPages.join('、') }}</p>
        <p>无有效文字层或文字过少的页码：{{ current.result.ocrPages.join('、') || '无' }}</p>
        <el-table :data="current.result.extractedFacts || []" border max-height="360">
          <el-table-column prop="name" label="原始指标" min-width="180" /><el-table-column prop="rawValue" label="原始值" width="100" />
          <el-table-column prop="valueKind" label="数值性质" width="110" /><el-table-column prop="year" label="年份" width="80" /><el-table-column prop="page" label="PDF页码" width="90" />
        </el-table>
        <el-collapse><el-collapse-item title="AI结构化响应（未经事实确认）"><pre class="diagnostic-json">{{ JSON.stringify(current.result.aiResponses || [], null, 2) }}</pre></el-collapse-item></el-collapse>
      </template>
    </el-dialog>
    <el-dialog v-model="adoptionVisible" title="确认写入待评估数据" width="700px" style="max-width: calc(100vw - 32px)" :close-on-click-modal="!saving">
      <el-alert title="采用将覆盖所选字段的原值，并清空旧转型评分，需要金融机构重新评估。" type="warning" :closable="false" />
      <el-table :data="adoptionRows" border><el-table-column prop="label" label="指标" /><el-table-column prop="expectedOldValue" label="原值" /><el-table-column prop="value" label="采用值" /></el-table>
      <el-input v-model="reviewNote" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="填写复核说明：主体、核算范围、年份和分母口径如何确认" />
      <el-checkbox v-model="subjectConfirmed">已核对报告主体、期间及核算范围属于本企业本条记录</el-checkbox>
      <template #footer><el-button @click="adoptionVisible = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="!subjectConfirmed || !reviewNote.trim()" @click="adopt">确认写入</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, DocumentChecked, Refresh, Setting, Check } from '@element-plus/icons-vue'
import axios from 'axios'
import request from '../api/request'

// 所有请求由服务端会话识别企业；不从localStorage拼接企业身份参数。
const template = ref({}), tasks = ref([]), current = ref(null), error = ref('')
const file = ref(null), uploadRef = ref(), year = ref(''), consent = ref(false), writeVisible = ref(false)
const hasCandidates = task => task.result?.candidates?.some(row => row.status !== 'MISSING') || false
const resultMode = ref('facts'), diagnosticVisible = ref(false)
const visibleCandidates = computed(() => (current.value?.result?.candidates || []).filter(row => resultMode.value === 'missing' ? row.status === 'MISSING' : row.status !== 'MISSING'))
const uploading = ref(false), refreshing = ref(false), templateVisible = ref(false)
const selected = ref([]), targets = ref([]), adopted = ref([]), targetId = ref(null), resultTable = ref()
const evidenceVisible = ref(false), evidence = ref(null), pdfUrl = ref(''), pdfLoading = ref(false)
const adoptionVisible = ref(false), adoptionRows = ref([]), saving = ref(false), reviewNote = ref(''), subjectConfirmed = ref(false)
const taskNames = { QUEUED: '排队中', PARSING: '解析材料', EXTRACTING: 'AI提取与核验', COMPLETED: '已完成', FAILED: '失败' }
const statusNames = { READY: '可采用', REVIEW: '需复核', BLOCKED: '不可直接采用', MISSING: '待补充', OUTSIDE: '模板外指标' }
const statusTypes = { READY: 'success', REVIEW: 'warning', BLOCKED: 'danger', MISSING: 'info', OUTSIDE: 'info' }
const processing = computed(() => current.value && ['QUEUED', 'PARSING', 'EXTRACTING'].includes(current.value.status))
let timer, alive = true, sequence = 0, pdfSequence = 0
function chooseFile(upload) {
  if (!upload.name.toLowerCase().endsWith('.pdf') || upload.size > 100 * 1024 * 1024) {
    ElMessage.warning('请选择100MB以内的PDF'); uploadRef.value?.clearFiles(); file.value = null; return
  }
  file.value = upload.raw
}
async function refresh() {
  refreshing.value = true; error.value = ''
  try { template.value = await request.get('/company-materials/template'); tasks.value = await request.get('/company-materials') }
  catch (e) { error.value = e.response?.data?.message || e.message }
  finally { refreshing.value = false }
}
async function start() {
  uploading.value = true
  try {
    const body = new FormData(); body.append('file', file.value); body.append('year', year.value); body.append('consent', consent.value)
    const data = await request.post('/company-materials', body, { timeout: 60000 })
    uploadRef.value?.clearFiles(); file.value = null
    await refresh(); await selectTask({ id: data.id })
  } catch { /* 统一拦截器展示失败，不产生虚假任务。 */ }
  finally { uploading.value = false }
}
async function selectTask(row) {
  const token = ++sequence; clearTimeout(timer)
  selected.value = []; targets.value = []; targetId.value = null; writeVisible.value = false; resultTable.value?.clearSelection()
  evidenceVisible.value = false; diagnosticVisible.value = false; ++pdfSequence
  if (pdfUrl.value) URL.revokeObjectURL(pdfUrl.value)
  pdfUrl.value = ''
  try {
    const data = await request.get(`/company-materials/${row.id}`)
    if (!alive || token !== sequence) return
    current.value = data
    if (data.result) {
      const targetData = await request.get(`/company-materials/${row.id}/targets`)
      if (!alive || token !== sequence) return
      targets.value = targetData.rows; adopted.value = targetData.adoptedRows
    }
    if (processing.value) timer = setTimeout(() => selectTask(row), 2500)
    else tasks.value = await request.get('/company-materials')
  } catch (e) { if (alive && token === sequence) error.value = e.response?.data?.message || e.message }
}
async function showEvidence(row) {
  evidence.value = row; evidenceVisible.value = true
  if (pdfUrl.value || pdfLoading.value) return
  const token = ++pdfSequence; const id = current.value.id; pdfLoading.value = true
  try {
    // PDF二进制不经过JSON响应拦截器，仍携带企业会话凭据。
    const response = await axios.get(`${request.defaults.baseURL}/company-materials/${id}/file`, { responseType: 'blob', withCredentials: true, timeout: 30000 })
    if (alive && token === pdfSequence) pdfUrl.value = URL.createObjectURL(response.data)
  } catch { ElMessage.error('原文加载失败，请重新登录或重试') }
  finally { pdfLoading.value = false }
}
function columnName(prop) { return prop.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`) }
function openAdoption() {
  const target = targets.value.find(row => row.id === targetId.value)
  if (!target || adopted.value.includes(target.id)) { ElMessage.warning('请选择尚未采用的目标记录'); return }
  adoptionRows.value = selected.value.map(row => ({ field: row.field, label: row.label, value: row.value, expectedOldValue: target[columnName(row.field)] ?? null }))
  reviewNote.value = ''; subjectConfirmed.value = false; adoptionVisible.value = true
}
async function adopt() {
  saving.value = true
  try {
    await request.post(`/company-materials/${current.value.id}/adoptions`, { rowId: targetId.value, subjectConfirmed: subjectConfirmed.value,
      items: adoptionRows.value.map(row => ({ ...row, reviewNote: reviewNote.value.trim() })) })
    adoptionVisible.value = false; ElMessage.success('已写入待评估记录，旧评分已清空'); await selectTask(current.value)
  } catch { /* 保留确认窗口，便于核对并发变更或权限错误。 */ }
  finally { saving.value = false }
}
onMounted(refresh)
onBeforeUnmount(() => { alive = false; ++sequence; ++pdfSequence; clearTimeout(timer); if (pdfUrl.value) URL.revokeObjectURL(pdfUrl.value) })
</script>

<style scoped>
.materials { background: #fff; padding: 22px; min-width: 0; }
header, .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
h2 { font-size: 20px; margin: 0 0 8px; } h3 { font-size: 16px; margin: 0; }
header span { color: #687785; } .upload-bar { margin-top: 24px; }
.toolbar { margin: 24px 0 12px; } .stats, .adoption-bar { display: flex; flex-wrap: wrap; gap: 18px; margin: 16px 0; }
.stats { color: #526777; font-size: 13px; } .el-alert { margin: 12px 0; }
.diagnostic-json { max-height: 350px; overflow: auto; white-space: pre-wrap; overflow-wrap: anywhere; }
.toolbar h3 { overflow-wrap: anywhere; min-width: 0; }
.materials :deep(.el-checkbox) { max-width: 100%; height: auto; min-height: 32px; }
.materials :deep(.el-checkbox__label) { white-space: normal; overflow-wrap: anywhere; line-height: 1.6; }
.evidence-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.3fr); gap: 20px; }
iframe { width: 100%; height: 580px; border: 1px solid #dce3e9; }
blockquote { margin: 16px 0; padding: 12px; background: #f3f6f8; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; }
li { margin: 8px 0; line-height: 1.6; } .el-textarea { margin: 18px 0; }
@media (max-width: 760px) { .materials { padding: 14px; } .evidence-layout { grid-template-columns: 1fr; } iframe { height: 380px; } }
</style>
