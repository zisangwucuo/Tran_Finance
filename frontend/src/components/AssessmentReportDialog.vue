<template>
  <el-dialog
    v-model="visibleModel"
    class="assessment-report-dialog"
    width="1180px"
    top="4vh"
    append-to-body
  >
    <template #header>
      <div class="report-dialog-header">
        <div>
          <strong>转型金融评估报告</strong>
          <span>{{ report?.reportNo || '-' }}</span>
        </div>
        <el-tag effect="plain" type="success">{{ report?.modelSource || '模型评分' }}</el-tag>
      </div>
    </template>

    <div v-if="report" class="report-scroll">
      <article ref="reportRef" class="report-paper">
        <section class="report-page cover-page">
          <div class="report-title-row">
            <div>
              <p class="report-kicker">TRANSITION FINANCE ASSESSMENT REPORT</p>
              <h1>{{ report.basicInfo?.companyCode || '未关联企业' }}转型金融评估报告</h1>
            </div>
            <div class="report-score">
              <span>评估得分</span>
              <strong>{{ formatNumber(report.basicInfo?.transitionScore) }}</strong>
            </div>
          </div>

          <div class="report-meta">
            <div v-for="item in basicInfoItems" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value || '-' }}</strong>
            </div>
          </div>

          <section class="report-section">
            <div class="section-heading">
              <span>01</span>
              <h2>各指标贡献值（权重）</h2>
            </div>
            <div class="chart-grid">
              <div class="chart-panel">
                <h3>图1-1 综合维度贡献值（通用层）</h3>
                <div ref="commonContributionRef" class="report-chart"></div>
              </div>
              <div class="chart-panel">
                <h3>图1-2 专项维度贡献值（特性层）</h3>
                <div ref="specialtyContributionRef" class="report-chart"></div>
              </div>
            </div>
          </section>

          <section class="report-section">
            <div class="section-heading">
              <span>02</span>
              <h2>各指标标准化得分（数值）</h2>
            </div>
            <div class="chart-grid">
              <div class="chart-panel">
                <h3>图2-1 综合维度标准化得分（通用层）</h3>
                <div ref="commonScoreRef" class="report-chart"></div>
              </div>
              <div class="chart-panel">
                <h3>图2-2 专项维度标准化得分（特性层）</h3>
                <div ref="specialtyScoreRef" class="report-chart"></div>
              </div>
            </div>
          </section>

          <footer class="report-footer">
            平台统一生成 · 报告编号 {{ report.reportNo }} · 评估年度 {{ report.basicInfo?.dataYear || '-' }} · 第 1 / 2 页
          </footer>
        </section>

        <section class="report-page narrative-page">
          <div class="section-heading narrative-heading">
            <span>03</span>
            <h2>评估结论与改进路径</h2>
          </div>

          <div class="narrative-summary">
            <h3>综合结论</h3>
            <p>{{ report.narrative?.summary }}</p>
            <el-tag effect="plain">{{ report.narrative?.source || '平台生成' }}</el-tag>
          </div>

          <div class="narrative-grid">
            <section>
              <h3>不足分析</h3>
              <ol>
                <li v-for="item in report.narrative?.weaknesses || []" :key="item">{{ item }}</li>
              </ol>
            </section>
            <section>
              <h3>总结与建议</h3>
              <ol>
                <li v-for="item in report.narrative?.suggestions || []" :key="item">{{ item }}</li>
              </ol>
            </section>
          </div>

          <div class="report-note">
            {{ report.narrative?.note }}
          </div>

          <footer class="report-footer">
            平台统一生成 · 报告编号 {{ report.reportNo }} · 仅供参考，不构成投资建议 · 第 2 / 2 页
          </footer>
        </section>
      </article>
    </div>

    <template #footer>
      <el-button @click="visibleModel = false">关闭</el-button>
      <el-button type="primary" @click="printReport">打印/另存PDF</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  report: {
    type: Object,
    default: null
  }
})
const emit = defineEmits(['update:modelValue'])

const reportRef = ref()
const commonContributionRef = ref()
const specialtyContributionRef = ref()
const commonScoreRef = ref()
const specialtyScoreRef = ref()
const isPrinting = ref(false)
let chartInstances = []

const visibleModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const basicInfoItems = computed(() => {
  const info = props.report?.basicInfo || {}
  return [
    { label: '企业编码', value: info.companyCode },
    
    { label: '所属行业', value: info.industryName },
    { label: '企业类型', value: info.companyType },
    { label: '评估年度', value: info.dataYear },
    { label: '评估机构', value: info.institutionName }
  ]
})

watch(
  () => [props.modelValue, props.report],
  async () => {
    if (props.modelValue && props.report) {
      await renderCharts()
    } else {
      disposeCharts()
    }
  },
  { flush: 'post' }
)

onMounted(() => {
  window.addEventListener('beforeprint', preparePrintCharts)
  window.addEventListener('afterprint', finishPrintReport)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeprint', preparePrintCharts)
  window.removeEventListener('afterprint', finishPrintReport)
  document.body.classList.remove('report-printing')
  disposeCharts()
})

async function renderCharts() {
  await nextTick()
  disposeCharts()
  chartInstances = [
    renderRadarChart(commonContributionRef.value, props.report.commonIndicators || [], 'contribution', '综合维度贡献值', '#2563eb'),
    renderRadarChart(specialtyContributionRef.value, props.report.specialtyIndicators || [], 'contribution', '专项维度贡献值', '#0f766e'),
    renderRadarChart(commonScoreRef.value, props.report.commonIndicators || [], 'normalizedScore', '综合维度标准化得分', '#7c3aed'),
    renderRadarChart(specialtyScoreRef.value, props.report.specialtyIndicators || [], 'normalizedScore', '专项维度标准化得分', '#d97706')
  ].filter(Boolean)
}

function renderRadarChart(target, items, valueKey, seriesName, color) {
  if (!target || !items.length) {
    return null
  }
  const chart = echarts.init(target)
  const maxValue = valueKey === 'normalizedScore' ? 100 : calculateContributionMax(items)
  chart.setOption({
    tooltip: {
      appendToBody: true,
      formatter: () => items.map((item) => {
        return [
          `${item.label}: ${formatNumber(item[valueKey])}`,
          `标准化得分 ${formatNumber(item.normalizedScore)}`,
          `模型权重 ${formatPercent(item.weight)}`,
          `原始值 ${formatCell(item.rawValue)}`
        ].join(' / ')
      }).join('<br/>')
    },
    radar: {
      radius: '50%',
      center: ['50%', '52%'],
      splitNumber: 4,
      indicator: items.map((item) => ({
        name: formatRadarAxisName(item.label),
        max: maxValue
      })),
      axisName: {
        color: '#334155',
        fontSize: 9,
        lineHeight: 12,
        width: 58,
        overflow: 'break'
      },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      splitArea: {
        areaStyle: {
          color: ['rgba(248, 250, 252, 0.72)', 'rgba(241, 245, 249, 0.72)']
        }
      }
    },
    series: [
      {
        name: seriesName,
        type: 'radar',
        areaStyle: { color: withAlpha(color, 0.16) },
        lineStyle: { color, width: 2 },
        itemStyle: { color },
        data: [
          {
            value: items.map((item) => Number(item[valueKey] || 0)),
            name: seriesName
          }
        ]
      }
    ]
  })
  return chart
}

function calculateContributionMax(items) {
  const max = Math.max(...items.map((item) => Number(item.contribution || 0)), 1)
  return Math.ceil(max * 1.25)
}

function withAlpha(color, alpha) {
  const normalized = color.replace('#', '')
  const red = parseInt(normalized.slice(0, 2), 16)
  const green = parseInt(normalized.slice(2, 4), 16)
  const blue = parseInt(normalized.slice(4, 6), 16)
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

function formatRadarAxisName(label) {
  const aliases = {
    转型目标与承诺: '目标\n承诺',
    转型战略与路径规划: '战略\n路径',
    治理架构与激励: '治理\n激励',
    转型技术应用现状: '技术\n应用',
    碳资产管理能力: '碳资产\n管理',
    风险管理与市场机遇: '风险\n机遇',
    信息披露与透明度: '信息\n披露',
    公正转型: '公正\n转型',
    环境合规: '环境\n合规',
    '销售净利率(%)': '销售\n净利率',
    销售净利率: '销售\n净利率',
    '营业收入增长率(%)': '收入\n增长率',
    营业收入增长率: '收入\n增长率',
    '研发投入强度(%)': '研发\n强度',
    研发投入强度: '研发\n强度',
    '清洁能源装机占比(%)': '清洁装机\n占比',
    清洁能源装机占比: '清洁装机\n占比',
    '清洁能源发电量占比(%)': '清洁发电\n占比',
    清洁能源发电占比: '清洁发电\n占比',
    供电标准煤耗: '供电\n煤耗',
    发电碳排放强度: '发电碳\n强度',
    '煤电灵活性改造比例(%)': '煤电灵活\n改造',
    煤电灵活性改造比例: '煤电灵活\n改造',
    '最小技术出力调峰深度(%)': '调峰\n深度',
    最小技术出力调峰深度: '调峰\n深度',
    '可再生能源投资额(亿元)': '可再生\n投资',
    可再生能源投资额: '可再生\n投资',
    高炉工序能耗: '高炉\n能耗',
    转炉工序能耗: '转炉\n能耗',
    电弧炉炼钢能耗: '电炉\n能耗',
    吨钢碳排放强度: '吨钢\n碳强',
    电炉短流程占比: '电炉\n占比',
    '电炉短流程占比(%)': '电炉\n占比',
    废钢利用量: '废钢\n利用',
    煤气利用率: '煤气\n利用',
    '煤气利用率(%)': '煤气\n利用',
    合成氨综合能耗: '合成氨\n能耗',
    乙烯裂解综合能耗: '乙烯\n能耗',
    甲醇综合能耗: '甲醇\n能耗',
    产品碳排放强度: '产品\n碳强',
    绿色电力使用比例: '绿电\n比例',
    '绿色电力使用比例(%)': '绿电\n比例',
    非化石原料使用比例: '非化石\n原料',
    '非化石原料使用比例(%)': '非化石\n原料',
    工业水重复利用率: '工业水\n复用',
    '工业水重复利用率(%)': '工业水\n复用',
    炼油综合能耗: '炼油\n能耗',
    炼油碳排放强度: '炼油\n碳强',
    甲烷排放强度: '甲烷\n排放',
    化工品收率占比: '化工品\n收率',
    '化工品收率占比(%)': '化工品\n收率',
    清洁能源使用比例: '清洁\n能源',
    '清洁能源使用比例(%)': '清洁\n能源',
    可持续航空燃料产销量: 'SAF\n产销',
    水泥熟料综合能耗: '熟料\n能耗',
    平板玻璃综合能耗: '玻璃\n能耗',
    替代燃料热替代率: '替代\n燃料',
    '替代燃料热替代率(%)': '替代\n燃料',
    替代原料使用比例: '替代\n原料',
    '替代原料使用比例(%)': '替代\n原料',
    电解铝绿电比例: '铝绿电\n比例',
    '电解铝绿电比例(%)': '铝绿电\n比例',
    电解铝直流电耗: '直流\n电耗',
    再生金属供应占比: '再生\n金属',
    '再生金属供应占比(%)': '再生\n金属',
    再生铝产量: '再生铝\n产量',
    '再生铝产量(万吨)': '再生铝\n产量',
    吨铝碳排放强度: '吨铝\n碳强',
    铝水直接合金化比例: '铝水\n合金',
    '铝水直接合金化比例(%)': '铝水\n合金',
    瓦斯抽采利用率: '瓦斯\n利用',
    '瓦斯抽采利用率(%)': '瓦斯\n利用',
    原煤综合能耗: '原煤\n能耗',
    矿井水利用率: '矿井水\n利用',
    '矿井水利用率(%)': '矿井水\n利用',
    固废综合利用率: '固废\n利用',
    '固废综合利用率(%)': '固废\n利用',
    非煤收入占比: '非煤\n收入',
    '非煤收入占比(%)': '非煤\n收入',
    新能源装机容量: '新能源\n装机',
    吨公里油耗: '吨公里\n油耗',
    吨公里CO2排放: '吨公里\nCO2',
    'RPK CO2排放': 'RPK\nCO2',
    SAF使用量: 'SAF\n使用',
    旅客吞吐量能耗: '吞吐量\n能耗',
    旅客吞吐量CO2排放: '吞吐量\nCO2',
    可再生能源消费占比: '可再生\n消费',
    '可再生能源消费占比(%)': '可再生\n消费',
    地面车辆电动化比例: '车辆\n电动化',
    '地面车辆电动化比例(%)': '车辆\n电动化',
    环保绩效评级: '环保\n评级'
  }
  if (aliases[label]) {
    return aliases[label]
  }
  const cleanLabel = String(label || '').replace(/\(.+?\)|（.+?）/g, '')
  if (cleanLabel.length <= 4) {
    return cleanLabel
  }
  if (cleanLabel.length <= 8) {
    return `${cleanLabel.slice(0, 4)}\n${cleanLabel.slice(4)}`
  }
  return `${cleanLabel.slice(0, 4)}\n${cleanLabel.slice(4, 8)}\n${cleanLabel.slice(8, 12)}`
}

function disposeCharts() {
  chartInstances.forEach((chart) => chart.dispose())
  chartInstances = []
}

async function printReport() {
  isPrinting.value = true
  document.body.classList.add('report-printing')
  await nextTick()
  preparePrintCharts()
  await new Promise((resolve) => window.requestAnimationFrame(resolve))
  window.print()
}

function resizeCharts() {
  chartInstances.forEach((chart) => chart.resize())
}

function preparePrintCharts() {
  const printOptions = {
    radar: {
      radius: '38%',
      center: ['50%', '54%'],
      axisName: {
        fontSize: 8,
        lineHeight: 10,
        width: 42,
        overflow: 'break'
      }
    }
  }
  chartInstances.forEach((chart) => {
    chart.setOption(printOptions)
    chart.resize()
  })
}

function restoreScreenCharts() {
  const screenOptions = {
    radar: {
      radius: '50%',
      center: ['50%', '52%'],
      axisName: {
        fontSize: 9,
        lineHeight: 12,
        width: 58,
        overflow: 'break'
      }
    }
  }
  chartInstances.forEach((chart) => {
    chart.setOption(screenOptions)
    chart.resize()
  })
}

function finishPrintReport() {
  document.body.classList.remove('report-printing')
  isPrinting.value = false
  restoreScreenCharts()
}

function formatNumber(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue.toFixed(1)
}

function formatPercent(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : `${(numberValue * 100).toFixed(2)}%`
}

function formatCell(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}
</script>

<style scoped>
.report-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.report-dialog-header strong {
  display: block;
  color: #1f2a44;
  font-size: 18px;
}

.report-dialog-header span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.report-scroll {
  max-height: 76vh;
  overflow: auto;
  background: #eef2f7;
  padding: 18px;
}

.report-paper {
  width: 1040px;
  margin: 0 auto;
  color: #1f2937;
}

.report-page {
  position: relative;
  min-height: 720px;
  padding: 34px 38px 56px;
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.12);
}

.report-page + .report-page {
  margin-top: 18px;
}

.report-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 18px;
  border-bottom: 2px solid #1f2a44;
}

.report-kicker {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.report-title-row h1 {
  margin: 0;
  color: #1f2a44;
  font-size: 30px;
  line-height: 1.25;
}

.report-score {
  min-width: 136px;
  padding: 12px 16px;
  color: #fff;
  background: #1f2a44;
  border-radius: 6px;
  text-align: center;
}

.report-score span {
  display: block;
  font-size: 13px;
}

.report-score strong {
  display: block;
  margin-top: 4px;
  font-size: 30px;
}

.report-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 20px 0;
}

.report-meta div {
  min-height: 58px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.report-meta span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.report-meta strong {
  display: block;
  margin-top: 6px;
  color: #23364f;
  font-size: 15px;
}

.report-section {
  margin-top: 20px;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.section-heading span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 26px;
  color: #fff;
  background: #2563eb;
  border-radius: 4px;
  font-weight: 800;
}

.section-heading h2 {
  margin: 0;
  color: #1f2a44;
  font-size: 18px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.chart-panel {
  overflow: hidden;
  padding: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.chart-panel h3 {
  margin: 0 0 8px;
  color: #334155;
  font-size: 13px;
}

.report-chart {
  height: 260px;
}

.narrative-page {
  display: flex;
  flex-direction: column;
}

.narrative-heading {
  margin-bottom: 20px;
}

.narrative-summary {
  padding: 18px 20px;
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-left: 4px solid #2563eb;
  border-radius: 6px;
}

.narrative-summary h3,
.narrative-grid h3 {
  margin: 0 0 10px;
  color: #1f2a44;
  font-size: 16px;
}

.narrative-summary p {
  margin: 0 0 12px;
  color: #334155;
  font-size: 15px;
  line-height: 1.8;
}

.narrative-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.narrative-grid section {
  padding: 16px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.narrative-grid ol {
  margin: 0;
  padding-left: 18px;
}

.narrative-grid li {
  margin-bottom: 10px;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
}

.report-note {
  margin-top: 18px;
  padding: 14px 16px;
  color: #475569;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.7;
}

.report-footer {
  position: absolute;
  left: 38px;
  right: 38px;
  bottom: 20px;
  color: #64748b;
  border-top: 1px solid #e2e8f0;
  padding-top: 10px;
  font-size: 12px;
  text-align: center;
}

@media print {
  @page {
    size: A4 portrait;
    margin: 10mm;
  }

  :global(html),
  :global(body) {
    width: auto !important;
    height: auto !important;
    min-height: 0 !important;
    overflow: visible !important;
    background: #fff !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  :global(body.report-printing > *:not(.el-overlay)) {
    display: none !important;
  }

  :global(body.report-printing .el-overlay:not(:has(.assessment-report-dialog))) {
    display: none !important;
  }

  :global(.el-overlay),
  :global(.el-overlay-dialog),
  :global(.el-dialog),
  :global(.el-dialog__body),
  :global(.assessment-report-dialog) {
    position: static !important;
    width: auto !important;
    height: auto !important;
    max-width: none !important;
    max-height: none !important;
    margin: 0 !important;
    padding: 0 !important;
    background: #fff !important;
    box-shadow: none !important;
    overflow: visible !important;
    transform: none !important;
  }

  :global(.el-dialog__header),
  :global(.el-dialog__footer) {
    display: none !important;
  }

  .report-scroll {
    max-height: none;
    overflow: visible;
    padding: 0;
    background: #fff;
  }

  .report-paper {
    display: block !important;
    width: 184mm;
    margin: 0 auto;
  }

  .report-page {
    display: block !important;
    width: 184mm;
    min-height: 277mm;
    padding: 7mm 8mm 13mm;
    box-sizing: border-box;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    overflow: visible;
    break-inside: avoid;
    page-break-inside: avoid;
  }

  .cover-page {
    break-after: page;
    page-break-after: always;
  }

  .narrative-page {
    break-before: auto;
    page-break-before: auto;
    break-after: auto;
    page-break-after: auto;
  }

  .report-page + .report-page {
    margin-top: 0;
  }

  .report-title-row h1 {
    font-size: 22px;
  }

  .report-score strong {
    font-size: 24px;
  }

  .report-meta {
    margin: 12px 0;
    gap: 7px;
  }

  .report-meta div {
    min-height: 48px;
    padding: 7px 9px;
  }

  .report-section {
    margin-top: 10px;
  }

  .section-heading {
    margin-bottom: 8px;
  }

  .chart-grid {
    gap: 9px;
  }

  .chart-panel {
    overflow: visible;
    padding: 7px;
  }

  .chart-panel h3 {
    margin-bottom: 4px;
    font-size: 11px;
  }

  .report-chart {
    width: 100%;
    height: 205px;
  }

  .narrative-summary {
    padding: 12px 14px;
  }

  .narrative-summary p,
  .narrative-grid li,
  .report-note {
    line-height: 1.55;
  }

  .narrative-grid {
    gap: 10px;
    margin-top: 10px;
  }

  .narrative-grid section {
    padding: 12px 14px;
  }

  .report-footer {
    left: 8mm;
    right: 8mm;
    bottom: 5mm;
  }
}
</style>
