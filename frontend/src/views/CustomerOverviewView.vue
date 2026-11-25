<template>
  <div class="customer-overview">
    <header class="overview-heading">
      <div>
        <h1>客户总览</h1>
        <p>{{ overview?.institutionName || '当前银行' }}<span class="heading-divider">/</span>{{ overview?.dataYear ? `${overview.dataYear}年` : '全部年份' }}客户与评估情况</p>
      </div>
      <div class="overview-controls">
        <label for="overview-year">数据年份</label>
        <el-select id="overview-year" v-model="selectedYear" aria-label="数据年份" :empty-values="[null, undefined]" placeholder="全部年份" :disabled="loading" @change="loadOverview">
          <el-option label="全部年份" value="" />
          <el-option v-for="year in overview?.years || []" :key="year" :label="`${year}年`" :value="year" />
        </el-select>
        <el-tooltip content="刷新总览"><el-button :icon="Refresh" :loading="loading" aria-label="刷新总览" @click="loadOverview" /></el-tooltip>
        <el-button :icon="QuestionFilled" text @click="scopeVisible = true">统计口径</el-button>
      </div>
    </header>

    <div v-if="error" class="overview-error" role="alert">
      <el-icon><Warning /></el-icon>
      <div><strong>暂时无法加载客户总览</strong><p>{{ error }}</p></div>
      <el-button :icon="Refresh" @click="loadOverview">重新加载</el-button>
    </div>
    <el-skeleton v-else-if="loading" :rows="12" animated class="overview-loading" />
    <template v-else-if="overview">
      <div class="overview-scope">
        <span><el-icon><OfficeBuilding /></el-icon>本行客户 · {{ totals.coveredIndustries }} 个行业</span>
        <span>更新于 {{ updatedAt }}</span>
      </div>
      <dl class="overview-kpis" aria-label="客户关键数据">
        <div v-for="item in kpis" :key="item.label" :class="item.tone">
          <dt><span>{{ item.label }}</span><el-icon><component :is="item.icon" /></el-icon></dt>
          <dd>{{ item.value }}<small>{{ item.unit }}</small></dd>
          <p>{{ item.note }}</p>
        </div>
      </dl>
      <el-alert v-if="totals.unresolvedRecords" class="identity-alert" type="warning" :closable="false"
        :title="`${formatNumber(totals.unresolvedRecords)} 条资料尚未关联企业编码，已计入资料数，暂不计入客户数。`" />

      <div v-if="!totals.records" class="overview-empty">
        <el-empty :description="selectedYear ? '该年份暂无客户资料' : '本行暂无客户资料'" />
        <el-button :icon="Plus" type="primary" @click="openData('pending')">录入待评估资料</el-button>
      </div>
      <template v-else>
        <section class="overview-visuals" aria-label="客户分布与评分">
          <div class="overview-panel industry-panel">
            <div class="overview-section-heading"><div><h2>客户分布在哪些行业</h2><p>按企业编码去重 · 单位：家</p></div><el-tag effect="plain">{{ totals.coveredIndustries }} 个行业</el-tag></div>
            <CustomerOverviewChart v-if="totals.customers" :option="industryOption" :description="industryDescription" />
            <el-empty v-else description="资料尚未关联企业编码" :image-size="90" />
          </div>
          <div class="overview-panel score-panel">
            <div class="overview-section-heading"><div><h2>转型评分分布</h2><p>{{ formatNumber(totals.scoredRecords) }} 条有效评分 · {{ formatNumber(totals.unscoredRecords) }} 条暂无有效评分</p></div></div>
            <div v-if="totals.scoredRecords" class="score-visual">
              <CustomerOverviewChart :option="scoreOption" :description="scoreDescription" />
              <ul class="score-legend">
                <li v-for="(band, index) in overview.scoreBands" :key="band.label"><span><i :style="{ background: scoreColors[index] }" />{{ band.label }}</span><strong>{{ formatNumber(band.count) }}<small>条</small></strong></li>
              </ul>
            </div>
            <el-empty v-else description="尚无有效评分，完成评估后显示" :image-size="90" />
            <p class="panel-footnote">分数分布不等同于风险等级或授信结论。</p>
          </div>
        </section>

        <section class="overview-operations" aria-label="客户变化与业务进度">
          <div class="overview-panel trend-panel">
            <div class="overview-section-heading"><div><h2>各年度客户覆盖</h2><p>每年有资料的企业数，同一企业跨年分别计数</p></div><span class="section-meta">{{ overview.yearly.length }} 个年份</span></div>
            <CustomerOverviewChart v-if="overview.yearly.length" :option="yearOption" :description="yearDescription" />
            <el-empty v-else description="暂无年度数据" :image-size="90" />
          </div>
          <div class="overview-panel progress-panel">
            <div class="overview-section-heading"><div><h2>评估进行到哪一步</h2><p>共 {{ formatNumber(totals.records) }} 条企业年度资料</p></div></div>
            <div class="workflow-track" role="img" :aria-label="stageDescription"><span v-for="stage in stages" :key="stage.label" :style="{ width: `${stage.count / totals.records * 100}%`, background: stage.color }" /></div>
            <div class="workflow-stages">
              <button v-for="stage in stages" :key="stage.label" @click="openData(stage.destination)">
                <span><i :style="{ background: stage.color }" />{{ stage.label }}</span><strong>{{ formatNumber(stage.count) }}<small>条</small></strong><el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <div class="followup-row">
              <el-icon><Bell /></el-icon><div><strong>{{ formatNumber(overview.notices.pending) }} 条补全通知待企业提交</strong><span>已提交 {{ formatNumber(overview.notices.submitted) }} 条</span></div>
              <el-button link type="primary" @click="openNotices">查看通知<el-icon><ArrowRight /></el-icon></el-button>
            </div>
          </div>
        </section>

        <section class="overview-details">
          <div class="overview-section-heading"><div><h2>各行业客户与资料</h2><p>客户数按行业分别去重，跨行业客户可能重复计入</p></div></div>
          <el-table :data="overview.industries" class="industry-detail-table" aria-label="各行业客户明细">
            <el-table-column prop="name" label="行业" min-width="140" />
            <el-table-column label="客户数（家）" min-width="125" align="right"><template #default="{ row }">{{ formatNumber(row.customers) }}</template></el-table-column>
            <el-table-column label="历史档案（条）" min-width="130" align="right"><template #default="{ row }">{{ formatNumber(row.historyRecords) }}</template></el-table-column>
            <el-table-column label="待评估资料（条）" min-width="140" align="right"><template #default="{ row }">{{ formatNumber(row.pendingRecords) }}</template></el-table-column>
            <el-table-column label="尚未评分（条）" min-width="130" align="right"><template #default="{ row }"><span :class="{ 'count-attention': row.pendingUnscored > 0 }">{{ formatNumber(row.pendingUnscored) }}</span></template></el-table-column>
            <el-table-column label="平均评分" min-width="110" align="right"><template #default="{ row }">{{ formatScore(row.averageScore) }}</template></el-table-column>
            <el-table-column label="资料" min-width="180" align="right"><template #default="{ row }"><el-button link type="primary" :disabled="!row.historyRecords" @click="openData('history', row.key)">历史档案</el-button><el-button link type="primary" :disabled="!row.pendingRecords" @click="openData('pending', row.key)">待评估</el-button></template></el-table-column>
          </el-table>
        </section>
      </template>
    </template>

    <el-dialog v-model="scopeVisible" title="这些数字如何计算" width="min(620px, calc(100vw - 32px))">
      <dl class="overview-definitions">
        <dt>统计范围</dt><dd>当前登录银行的八个行业历史档案和待评估资料，统计全部符合年份条件的记录，不受列表分页限制。</dd>
        <dt>客户与资料的区别</dt><dd>客户数按企业编码去重。一家企业有三年的资料，计为1家客户、3条资料；尚未关联编码的资料不计入客户数。</dd>
        <dt>评分口径</dt><dd>仅统计0至100分的有效评分，0分会参与计算。平均评分按资料逐条计算，未评分和异常值不计入；分数区间为低于40、40至不足60、60至不足80、80至100分。</dd>
        <dt>评估进度</dt><dd>历史档案是已经归档的记录。待评估资料分为尚未评分和已有评分两类；已有评分不代表已授信。历史档案也可能没有评分。</dd>
        <dt>通知数量</dt><dd>统计当前年份条件下的补全通知，单位为条，不能当作企业数或未评分资料数。</dd>
      </dl>
      <template #footer><el-button type="primary" @click="scopeVisible = false">知道了</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Bell, DataAnalysis, Document, OfficeBuilding, Plus, QuestionFilled, Refresh, Timer, Warning } from '@element-plus/icons-vue'
import { fetchCustomerOverview } from '../api/customerOverview'
import CustomerOverviewChart from '../components/CustomerOverviewChart.vue'
import { createCustomerOverviewPresentation } from '../utils/customerOverviewPresentation'

const router = useRouter()
const liveOverview = ref(null), selectedYear = ref(''), loading = ref(true), error = ref(''), scopeVisible = ref(false)
// 展示调整仅存在于当前视图，其他业务页面仍读取未经变更的接口数据。
const overview = computed(() => createCustomerOverviewPresentation(liveOverview.value))
let requestSequence = 0, disposed = false
const totals = computed(() => overview.value.totals)
const formatNumber = value => Number(value || 0).toLocaleString('zh-CN')
const formatScore = value => value === null || value === undefined ? '-' : Number(value).toFixed(1)
const updatedAt = computed(() => new Date(overview.value.generatedAt).toLocaleString('zh-CN', { hour12: false }))
const scoreColors = ['#c88065', '#d5ad60', '#548db5', '#278c7d']
const kpis = computed(() => [
  { label: '服务客户', value: formatNumber(totals.value.customers), unit: '家', note: '本行资料中的去重企业数', icon: OfficeBuilding, tone: 'teal' },
  { label: '企业年度资料', value: formatNumber(totals.value.records), unit: '条', note: `历史 ${formatNumber(totals.value.historyRecords)} · 待评估 ${formatNumber(totals.value.pendingRecords)}`, icon: Document, tone: 'blue' },
  { label: '待评估资料中尚未评分', value: formatNumber(totals.value.pendingUnscored), unit: '条', note: '等待完成模型评估', icon: Timer, tone: 'amber' },
  { label: '平均转型评分', value: formatScore(totals.value.averageScore), unit: totals.value.averageScore === null ? '' : '分', note: `来自 ${formatNumber(totals.value.scoredRecords)} 条有效评分`, icon: DataAnalysis, tone: 'neutral' }
])
const stages = computed(() => [
  { label: '待评估 · 尚未评分', count: totals.value.pendingUnscored, color: '#d5ad60', destination: 'pending' },
  { label: '待评估 · 已有评分', count: totals.value.pendingScored, color: '#278c7d', destination: 'pending' },
  { label: '已归档 · 历史资料', count: totals.value.historyRecords, color: '#548db5', destination: 'history' }
])
const stageDescription = computed(() => stages.value.map(item => `${item.label}${item.count}条`).join('，'))
const industryDescription = computed(() => overview.value.industries.map(item => `${item.name}${item.customers}家`).join('，'))
const scoreDescription = computed(() => overview.value.scoreBands.map(item => `${item.label}${item.count}条`).join('，'))
const yearDescription = computed(() => overview.value.yearly.map(item => `${item.year}年${item.customers}家客户`).join('，'))
// Canvas 内的提示使用富文本模式，避免将服务端数据拼接为 HTML。
const chartBase = { animationDuration: 400, textStyle: { fontFamily: 'Microsoft YaHei, Arial, sans-serif', color: '#647b80' }, tooltip: { renderMode: 'richText', confine: true } }
const industryOption = computed(() => ({
  ...chartBase, grid: { left: 8, right: 45, top: 8, bottom: 12, containLabel: true },
  tooltip: { ...chartBase.tooltip, trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: value => `${value} 家客户` },
  xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef2f1', type: 'dashed' } }, axisLabel: { fontSize: 11 } },
  yAxis: { type: 'category', inverse: true, data: overview.value.industries.map(item => item.name), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontSize: 12 } },
  series: [{ name: '客户数', type: 'bar', barMaxWidth: 14, data: overview.value.industries.map(item => item.customers), itemStyle: { color: '#278c7d', borderRadius: [0, 3, 3, 0] }, label: { show: true, position: 'right', color: '#31554e', fontSize: 12 } }]
}))
const scoreOption = computed(() => ({
  ...chartBase, color: scoreColors,
  tooltip: { ...chartBase.tooltip, trigger: 'item', formatter: '{b}\n{c} 条（{d}%）' },
  title: { text: formatNumber(totals.value.scoredRecords), subtext: '有效评分 / 条', left: 'center', top: '37%', textStyle: { fontSize: 26, color: '#294b45', fontWeight: 600 }, subtextStyle: { fontSize: 11, color: '#78908a' } },
  series: [{ type: 'pie', radius: ['64%', '85%'], center: ['50%', '50%'], avoidLabelOverlap: true, label: { show: false }, itemStyle: { borderColor: '#fff', borderWidth: 3 }, data: overview.value.scoreBands.map(item => ({ name: item.label, value: item.count })) }]
}))
const yearOption = computed(() => ({
  ...chartBase, grid: { left: 10, right: 28, top: 40, bottom: 15, containLabel: true },
  tooltip: { ...chartBase.tooltip, trigger: 'axis', valueFormatter: value => `${value} 家客户` },
  xAxis: { type: 'category', data: overview.value.yearly.map(item => String(item.year)), axisTick: { show: false }, axisLine: { lineStyle: { color: '#dfe8e6' } }, axisLabel: { fontSize: 11 } },
  yAxis: { type: 'value', name: '客户 / 家', minInterval: 1, splitLine: { lineStyle: { color: '#edf2f1', type: 'dashed' } }, axisLabel: { fontSize: 11 } },
  // 使用直线连接年度点并标注数值，不做平滑插值。
  series: [{ name: '年度客户数', type: 'line', smooth: false, symbol: 'circle', symbolSize: 7, data: overview.value.yearly.map(item => item.customers), itemStyle: { color: '#548db5' }, lineStyle: { width: 2 }, areaStyle: { color: '#e9f2f8', opacity: .5 }, label: { show: true, position: 'top', color: '#456d8a', fontSize: 11 } }]
}))

async function loadOverview() {
  const sequence = ++requestSequence
  error.value = ''
  loading.value = true
  try {
    const result = await fetchCustomerOverview(selectedYear.value)
    if (!disposed && sequence === requestSequence) liveOverview.value = result
  } catch (exception) {
    // 加载失败时不继续显示上一次年份的数据，避免误认为新筛选结果。
    if (!disposed && sequence === requestSequence) error.value = exception.response?.data?.message || exception.message || '请稍后重试'
  } finally {
    if (!disposed && sequence === requestSequence) loading.value = false
  }
}

// 保留总览年份与行业条件，跳转后由原有业务页面完成数据读取和权限校验。
function openData(dataset, industryKey) {
  const query = {}
  if (selectedYear.value) query.dataYear = String(selectedYear.value)
  const availableIndustry = overview.value?.industries.find(item => dataset === 'history' ? item.historyRecords > 0 : item.pendingRecords > 0)
  if (industryKey || availableIndustry) query.industryKey = industryKey || availableIndustry.key
  router.push({ path: `/dashboard/${dataset === 'history' ? 'electric-company-data' : 'pending-company-data'}`, query })
}
function openNotices() {
  router.push({ path: '/dashboard/data-notices', query: { status: 'PENDING', ...(selectedYear.value ? { dataYear: String(selectedYear.value) } : {}) } })
}
onMounted(loadOverview)
onBeforeUnmount(() => { disposed = true; requestSequence++ })
</script>

<style scoped>
.customer-overview { padding: 24px 28px 32px; color: #29443f; min-width: 0; }
.overview-heading, .overview-controls, .overview-scope, .overview-section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.overview-heading { flex-wrap: wrap; padding-bottom: 20px; }
h1 { font-size: 23px; margin: 0; font-weight: 700; }
.overview-heading p { font-size: 13px; color: #758983; margin: 8px 0 0; }
.heading-divider { padding: 0 10px; color: #b3c2be; }
.overview-controls { gap: 8px; flex-wrap: wrap; justify-content: flex-start; }
.overview-controls label { font-size: 12px; color: #6c817b; margin-right: 4px; }
.overview-controls .el-select { width: 132px; }
.overview-controls .el-button + .el-button { margin-left: 0; }
.overview-scope { padding: 14px 20px; background: #eaf3ef; color: #54776b; font-size: 12px; flex-wrap: wrap; }
.overview-scope span:first-child { display: flex; align-items: center; gap: 8px; }
.overview-kpis { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); background: #fff; margin: 0 0 20px; padding: 22px 0; border-bottom: 1px solid #dfe9e5; }
.overview-kpis > div { padding: 0 24px; border-right: 1px solid #e8eeec; min-width: 0; }
.overview-kpis > div:last-child { border: 0; }
.overview-kpis dt { display: flex; justify-content: space-between; gap: 10px; color: #687e77; font-size: 13px; }
.overview-kpis dt .el-icon { font-size: 19px; color: #7b9d90; flex-shrink: 0; }
.overview-kpis dd { margin: 14px 0 8px; font-size: 32px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; color: #237b6c; }
.overview-kpis small, .score-legend small, .workflow-stages small { font-size: 12px; margin-left: 6px; color: #85978f; font-weight: 400; }
.overview-kpis p { margin: 0; font-size: 12px; color: #6b8075; line-height: 1.6; }
.overview-kpis .blue dd { color: #4d80a5; }
.overview-kpis .amber dd { color: #b0873f; }
.overview-kpis .neutral dd { color: #536e75; }
.overview-visuals, .overview-operations { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); background: #fff; margin-bottom: 20px; border-bottom: 1px solid #e1e9e5; }
.overview-panel { padding: 22px 24px; min-width: 0; }
.overview-panel + .overview-panel { border-left: 1px solid #e9eeec; }
.overview-section-heading { align-items: flex-start; gap: 10px; margin-bottom: 16px; }
.overview-section-heading h2 { font-size: 16px; margin: 0; font-weight: 600; }
.overview-section-heading p, .panel-footnote { color: #687e73; font-size: 12px; margin: 7px 0 0; line-height: 1.7; }
.section-meta { color: #81998d; font-size: 12px; white-space: nowrap; }
.score-visual { display: grid; grid-template-columns: minmax(160px, 1.1fr) minmax(135px, 1fr); align-items: center; gap: 10px; min-height: 270px; }
.score-visual :deep(.overview-chart) { height: 240px; }
.score-legend { list-style: none; padding: 0; margin: 0; display: grid; gap: 20px; font-size: 12px; }
.score-legend li, .score-legend li > span { display: flex; align-items: center; gap: 8px; }
.score-legend li { justify-content: space-between; }
.score-legend strong { font-size: 15px; white-space: nowrap; }
.score-legend i, .workflow-stages i:not(.el-icon) { width: 8px; height: 8px; display: inline-block; border-radius: 2px; flex-shrink: 0; }
.panel-footnote { margin-top: 8px; }
.trend-panel :deep(.overview-chart) { height: 258px; }
.workflow-track { display: flex; width: 100%; height: 10px; border-radius: 3px; overflow: hidden; background: #eef2f0; margin: 26px 0 12px; }
.workflow-stages { display: grid; }
.workflow-stages button { display: flex; align-items: center; gap: 12px; padding: 14px 0; border: 0; border-bottom: 1px solid #f0f3f1; background: transparent; color: #536f64; text-align: left; cursor: pointer; }
.workflow-stages button:hover { color: #177b78; background: #f8fbf9; }
.workflow-stages button > span { display: flex; align-items: center; gap: 8px; flex: 1; font-size: 13px; }
.workflow-stages strong { font-size: 19px; font-weight: 600; }
.workflow-stages .el-icon { color: #a0b5a8; font-size: 12px; }
.followup-row { display: flex; align-items: center; gap: 10px; margin-top: 18px; }
.followup-row > .el-icon { color: #b0873f; font-size: 21px; }
.followup-row div { flex: 1; min-width: 0; }
.followup-row strong { font-size: 12px; font-weight: 500; display: block; }
.followup-row span { display: block; font-size: 11px; color: #8a9a92; margin-top: 6px; }
.overview-details { background: #fff; padding: 22px 24px; min-width: 0; }
.count-attention { color: #ae8034; font-weight: 600; }
.identity-alert { margin-bottom: 18px; }
.overview-empty { text-align: center; padding: 12px 16px 40px; background: #fff; }
.overview-error { display: flex; flex-wrap: wrap; align-items: center; gap: 16px; padding: 24px; background: #fff; border-top: 2px solid #c9945c; }
.overview-error > .el-icon { font-size: 26px; color: #bd9256; }
.overview-error div { flex: 1; min-width: 200px; }
.overview-error p { font-size: 13px; color: #73847a; }
.overview-loading { padding: 32px 24px; background: #fff; }
.overview-definitions { margin: 0; line-height: 1.8; }
.overview-definitions dt { font-weight: 600; margin-top: 14px; }
.overview-definitions dd { margin: 4px 0 0; color: #64776f; }
@media (max-width: 1200px) {
  .customer-overview { padding: 20px; }
  .overview-kpis > div { padding: 0 16px; }
  .overview-panel { padding: 20px 16px; }
  .score-visual { grid-template-columns: minmax(0, 1fr); gap: 0; }
  .score-visual :deep(.overview-chart) { height: 190px; }
  .score-legend { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px; }
  .overview-kpis dt .el-icon { display: none; }
}
@media (max-width: 760px) {
  .customer-overview { padding: 16px 12px; }
  h1 { font-size: 20px; }
  .overview-visuals, .overview-operations { grid-template-columns: minmax(0, 1fr); }
  .overview-panel + .overview-panel { border-left: 0; border-top: 1px solid #e9eeec; }
  .overview-kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); row-gap: 22px; }
  .overview-kpis > div:nth-child(2) { border-right: 0; }
  .overview-kpis dd { font-size: 26px; }
  .overview-scope { gap: 8px; padding: 12px 16px; }
  .overview-details { padding: 20px 12px; }
  .score-visual { grid-template-columns: minmax(0, 1fr); }
  .score-visual :deep(.overview-chart) { height: 225px; }
  .score-legend { gap: 16px; }
  .followup-row { flex-wrap: wrap; }
}
</style>
