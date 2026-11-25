<template>
  <div class="comparison-page">
    <div class="comparison-heading">
      <h2>模型效果对比</h2>
      <el-button :icon="QuestionFilled" @click="helpVisible = true">指标说明</el-button>
    </div>
    <section class="comparison-toolbar">
      <el-radio-group v-model="mode" :disabled="busy || submitting" aria-label="对比模式">
        <el-radio-button value="default">默认参数对比</el-radio-button>
        <el-radio-button value="tuned">自动调参对比</el-radio-button>
        <el-radio-button value="optimized">原始指标优化</el-radio-button>
      </el-radio-group>
      <el-select v-model="industry" aria-label="实验行业" style="width: 180px">
        <el-option label="全部行业" value="all" />
        <el-option v-for="item in industryOptions" :key="item.key" :label="item.name" :value="item.key" />
      </el-select>
      <el-button type="primary" :icon="DataAnalysis" :loading="submitting" :disabled="busy || loading" @click="start">开始对比</el-button>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      <el-button :icon="Download" :disabled="!rows.length" @click="exportCsv">导出 CSV</el-button>
    </section>
    <section class="comparison-results">
      <el-select v-model="selectedId" placeholder="选择实验记录" aria-label="实验记录" style="width: min(100%, 460px)">
        <el-option v-for="run in runs" :key="run.id" :value="run.id" :label="`${new Date(run.createdAt).toLocaleString()} · ${modeNames[run.mode] || '默认参数'} · ${statusNames[run.status] || run.status}`" />
      </el-select>
      <template v-if="selected">
        <p>{{ selected.split }} · {{ selected.tuning }} · 随机种子 {{ selected.seed }} · 分类阈值 {{ selected.threshold }}</p>
        <el-progress :percentage="selected.progress" :status="selected.status === 'FAILED' ? 'exception' : selected.status === 'SUCCESS' ? 'success' : undefined" />
        <p>{{ modelDisplay(selected.message) }}</p>
        <el-alert v-for="failure in selected.failures || []" :key="failure.industryKey" :title="`${industryName(failure.industryKey)}：${failure.reason}`" type="warning" :closable="false" />
      </template>
      <el-empty v-else description="暂无对比实验" />
      <template v-if="rows.length">
        <div class="chart-toolbar">
          <el-select v-model="chartIndustry" aria-label="图表行业" style="width: 180px">
            <el-option v-for="key in resultIndustries" :key="key" :label="industryName(key)" :value="key" />
          </el-select>
          <el-select v-model="metric" aria-label="对比指标" style="width: 200px">
            <el-option v-for="item in metrics" :key="item.key" :label="item.label" :value="item.key" />
          </el-select>
        </div>
        <div ref="chartElement" class="comparison-chart" />
        <el-table :data="rows" border style="width: 100%">
          <el-table-column prop="industryName" label="行业" width="135" fixed />
          <el-table-column label="模型" width="115" fixed><template #default="{ row }">{{ modelDisplay(row.model) }}</template></el-table-column>
          <el-table-column label="训练年份" width="135"><template #default="{ row }">{{ row.trainYears.join('、') }}</template></el-table-column>
          <el-table-column prop="testYear" label="测试年份" width="90" />
          <el-table-column prop="trainSamples" label="训练样本" width="90" />
          <el-table-column prop="testSamples" label="测试样本" width="90" />
          <el-table-column v-for="item in metrics" :key="item.key" :label="item.short" width="110">
            <template #default="{ row }">{{ row[item.key] == null ? '不可计算' : Number(row[item.key]).toFixed(4) }}</template>
          </el-table-column>
          <el-table-column label="详情" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="detail = row">查看</el-button></template></el-table-column>
        </el-table>
      </template>
    </section>
    <el-dialog v-model="helpVisible" title="模型评价指标说明" width="min(880px, calc(100vw - 32px))" top="6vh">
      <div class="metric-help">
        <p>本实验将“经核验转型绩效=1”视为正类，0视为负类。标签表示历史记录中的类别，不等同于企业真实违约风险或最终授信决定。</p>
        <h3>先理解混淆矩阵</h3>
        <p>TP：实际为1、预测为1；FP：实际为0、预测为1；FN：实际为1、预测为0；TN：实际为0、预测为0。详情中的矩阵以真实类别为行、预测类别为列。</p>
        <section v-for="item in metricGuide" :key="item.title" class="metric-definition">
          <h3>{{ item.title }}</h3>
          <p>{{ item.definition }}</p>
          <p class="metric-standard">判断标准：{{ item.standard }}</p>
        </section>
        <h3>本项目怎样比较</h3>
        <p>列表与图表的分类指标默认使用0.5阈值。详情另外展示按训练年份内部验证F1选择阈值后的结果；最终测试年份不参与阈值选择。AUC、AP、LogLoss和Brier由预测概率计算，不随分类阈值改变。</p>
        <p>比较应使用相同银行、行业、测试年份、样本与标签。不存在通用的“达到某个分数就合格”：需要结合类别比例、误判成本、样本量和跨年份稳定性判断。模拟数据上的表现不能直接当作真实授信能力证明。</p>
      </div>
      <template #footer><el-button type="primary" @click="helpVisible = false">我知道了</el-button></template>
    </el-dialog>
    <el-dialog :model-value="!!detail" title="实验详情" width="min(700px, calc(100vw - 32px))" @close="detail = null">
      <template v-if="detail">
        <p>{{ detail.industryName }} · {{ modelDisplay(detail.model) }}</p>
        <template v-if="detail.fixedThreshold">
          <p>测试集预测概率：最低 {{ number(detail.probabilityRange.min) }}，最高 {{ number(detail.probabilityRange.max) }}，平均 {{ number(detail.probabilityRange.mean) }}。</p>
          <section v-for="entry in thresholdDetails" :key="entry.label" class="threshold-section">
            <h3>{{ entry.label }} · 阈值 {{ number(entry.value.threshold) }}</h3>
            <p>预测正例 {{ entry.value.predictedPositive }} 条；精确率 {{ number(entry.value.precision) }}；召回率 {{ number(entry.value.recall) }}；F1 {{ number(entry.value.f1) }}；准确率 {{ number(entry.value.accuracy) }}。</p>
            <el-alert v-if="entry.value.precisionReason" :title="entry.value.precisionReason" type="warning" :closable="false" />
            <el-alert v-if="entry.value.recallReason" :title="entry.value.recallReason" type="warning" :closable="false" />
            <table class="confusion-matrix">
              <thead><tr><th>真实类别 / 预测类别</th><th>预测0</th><th>预测1</th></tr></thead>
              <tbody><tr><th>真实0</th><td>{{ entry.value.confusionMatrix[0][0] }}（TN）</td><td>{{ entry.value.confusionMatrix[0][1] }}（FP）</td></tr>
                <tr><th>真实1</th><td>{{ entry.value.confusionMatrix[1][0] }}（FN）</td><td>{{ entry.value.confusionMatrix[1][1] }}（TP）</td></tr></tbody>
            </table>
          </section>
          <p v-if="detail.thresholdSelection.available">阈值选择：{{ detail.thresholdSelection.rule }}；验证年份 {{ detail.thresholdSelection.validationYears.join('、') }}；验证样本 {{ detail.thresholdSelection.validationSamples }} 条；验证F1 {{ number(detail.thresholdSelection.validationF1) }}。</p>
          <el-alert v-else :title="detail.thresholdSelection.reason" type="info" :closable="false" />
        </template>
        <el-alert v-else title="旧实验未保存混淆矩阵和阈值诊断，请重新运行实验查看；原有指标保持不变。" type="info" :closable="false" />
        <p>训练企业 {{ detail.trainCompanies }} 家，测试企业 {{ detail.testCompanies }} 家；训练正例 {{ detail.trainPositive }} 条，测试正例 {{ detail.testPositive }} 条。</p>
        <p>指标 {{ detail.featureCount }} 个；训练集缺失单元格 {{ detail.missingTrainValues }} 个；无效标签或年份排除 {{ detail.excludedSamples }} 条。</p>
        <p>缺失值：训练集中位数填补，全缺失列填0；逻辑回归额外标准化。</p>
        <p v-if="detail.featureMode === 'raw_with_missing_indicators'">输入：未截断原始数值，评级与布尔固定编码，附加训练集缺失标记。</p>
        <template v-if="detail.tuning">
          <p>候选配置 {{ detail.tuning.candidateCount }} 组；内部验证年份 {{ detail.tuning.validationYears.join('、') }}；平均验证 AUC {{ detail.tuning.validationAuc.toFixed(4) }}。</p>
          <p>搜索耗时 {{ detail.searchSeconds.toFixed(2) }} 秒；下方为按内部验证选择的参数。</p>
          <el-collapse><el-collapse-item title="候选配置及验证结果"><pre>{{ JSON.stringify(detail.tuning.trials, null, 2) }}</pre></el-collapse-item></el-collapse>
        </template>
        <el-alert v-if="detail.warning" :title="detail.warning" type="warning" :closable="false" />
        <pre>{{ JSON.stringify(detail.parameters, null, 2) }}</pre>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { modelDisplay } from '../utils/modelDisplay'
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { DataAnalysis, Download, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '../api/request'
import { industryOptions } from '../utils/industryOptions'

const runs = ref([]), selectedId = ref(''), industry = ref('all'), submitting = ref(false), loading = ref(false)
const mode = ref('default')
const helpVisible = ref(false)
// 指标解释与当前二分类计算口径保持一致，不用任意分数线代替业务验收标准。
const metricGuide = [
  { title: '精确率 Precision', definition: 'TP / (TP + FP)：模型预测为经核验转型绩效正类的记录中，实际为正类的比例。', standard: '0–1，同等条件下越高表示误报越少。没有预测正例时分母为0，显示“不可计算”，不等于0。' },
  { title: '召回率 Recall', definition: 'TP / (TP + FN)：经核验转型绩效为正的记录中，被模型识别出来的比例。', standard: '0–1，越高表示漏报越少。测试集没有实际正例时不可计算；提高召回率可能增加误报。' },
  { title: '分类阈值', definition: '预测概率大于或等于阈值时判为1，否则判为0。默认阈值为0.5，它不是模型准确率。', standard: '不以越高或越低为好。降低阈值通常增加预测正例、提高或保持召回率，精确率不保证单调变化。应依据验证数据和误判成本选择，不能用最终测试集挑选。' },
  { title: 'F1', definition: '2TP / (2TP + FP + FN)：精确率与召回率的调和平均。', standard: '0–1，越高越好，但不考虑真负例，也不区分两类错误的业务成本。实际和预测均无正例时不可计算。' },
  { title: '准确率 Accuracy', definition: '(TP + TN) / 全部测试样本数：所有预测中判对的比例。', standard: '0–1，越高越好。类别不平衡时可能误导，例如90%为负类，全部预测为0也能得到90%的准确率。' },
  { title: 'ROC-AUC', definition: '衡量模型将正例排在负例之前的能力，与某个固定分类阈值无关。', standard: '0–1；0.5是随机排序的参考水平，1是完美排序，低于0.5需检查方向及泛化情况。测试集必须同时有正负类；高AUC不保证0.5阈值下分类好或概率准确。' },
  { title: '平均精确率 AP', definition: '按召回率增量加权汇总精确率，概括精确率–召回率表现；不是准确率，也不是简单算术平均。', standard: '0–1，越高越好。随机排序的参考水平与正类比例有关，不能一律用0.5作标准；本项目测试集只有一个类别时不计算。' },
  { title: '对数损失 LogLoss', definition: '衡量预测概率与实际标签的偏差，对“非常自信但预测错误”的情况惩罚较大。', standard: '越低越好，理想值为0，无固定上限。需在相同测试集上比较，不能使用通用合格分数。' },
  { title: 'Brier分数', definition: '所有测试样本的 (预测为1的概率 − 实际0/1标签)² 的平均值。', standard: '本项目二分类口径范围0–1，越低越好。固定预测0.5时为0.25，但0.25不是所有数据集的统一合格线。' },
  { title: '预测正例数与概率范围', definition: '预测正例数是概率达到阈值的记录数；概率范围包含测试预测的最小值、最大值和均值。', standard: '数量或概率高低本身不表示模型优劣。最大概率低于阈值时没有预测正例，可解释精确率不可计算、召回率为0的情况。' },
  { title: '训练、调参与预测耗时', definition: '训练耗时指选定参数后的最终拟合；调参耗时指参数搜索；预测耗时指整批测试样本预测，单位分别为秒、秒和毫秒。', standard: '在相同硬件、负载和样本规模下越低越省计算资源，但不能以速度代替预测质量。阈值选择耗时单独记录在CSV中。' }
]
const modeNames = { default: '默认参数', tuned: '自动调参', optimized: '原始指标优化' }
const number = value => value == null ? '不可计算' : Number(value).toFixed(4)
const chartIndustry = ref(''), metric = ref('auc'), chartElement = ref(null), detail = ref(null)
const statusNames = { PENDING: '等待中', RUNNING: '运行中', SUCCESS: '已完成', PARTIAL: '部分完成', FAILED: '失败' }
const thresholdDetails = computed(() => detail.value?.fixedThreshold ? [
  { label: '固定阈值结果', value: detail.value.fixedThreshold },
  ...(detail.value.selectedThreshold ? [{ label: '内部验证阈值结果', value: detail.value.selectedThreshold }] : [])
] : [])
const metrics = [
  { key: 'auc', label: 'ROC-AUC（越高越好）', short: 'ROC-AUC' },
  { key: 'ap', label: 'AP（越高越好）', short: 'AP' },
  { key: 'f1', label: 'F1（越高越好）', short: 'F1' },
  { key: 'accuracy', label: '准确率', short: '准确率' },
  { key: 'precision', label: '精确率', short: '精确率' },
  { key: 'recall', label: '召回率', short: '召回率' },
  { key: 'logLoss', label: 'LogLoss（越低越好）', short: 'LogLoss' },
  { key: 'brier', label: 'Brier（越低越好）', short: 'Brier' },
  { key: 'trainSeconds', label: '训练耗时（秒）', short: '训练秒数' },
  { key: 'searchSeconds', label: '调参耗时（秒）', short: '调参秒数' },
  { key: 'predictMs', label: '测试集预测耗时（毫秒）', short: '预测毫秒' }
]
const selected = computed(() => runs.value.find(run => run.id === selectedId.value))
const rows = computed(() => selected.value?.results || [])
const resultIndustries = computed(() => [...new Set(rows.value.map(row => row.industryKey))])
const busy = computed(() => runs.value.some(run => ['PENDING', 'RUNNING'].includes(run.status)))
const industryName = key => industryOptions.find(item => item.key === key)?.name || key
let timer, chart, observer, disposed = false

// 只轮询当前机构的实验记录，离开页面停止轮询和图表观察器。
async function load() {
  if (loading.value) return
  loading.value = true
  try {
    const data = await request.get('/model-comparisons')
    if (disposed) return
    runs.value = data
    if (!runs.value.some(run => run.id === selectedId.value)) selectedId.value = runs.value[0]?.id || ''
  } catch { /* 统一请求拦截器展示错误，不产生未处理Promise。 */ }
  finally { loading.value = false }
}
async function start() {
  submitting.value = true
  try {
    const result = await request.post('/model-comparisons', { industryKey: industry.value, mode: mode.value })
    await load()
    selectedId.value = result.id
    ElMessage.success('对比实验已启动')
  } catch { /* 由请求拦截器提示。 */ }
  finally { submitting.value = false }
}
watch([rows, metric, chartIndustry], async () => {
  if (!resultIndustries.value.includes(chartIndustry.value)) chartIndustry.value = resultIndustries.value[0] || ''
  await nextTick()
  if (disposed) return
  if (!chartElement.value) { chart?.dispose(); chart = null; observer?.disconnect(); return }
  if (!chart) {
    chart = echarts.init(chartElement.value)
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartElement.value)
  }
  const data = rows.value.filter(row => row.industryKey === chartIndustry.value)
  chart.setOption({ title: { subtext: '界面别名；当前指标来自未替换的后端算法', left: 'center' }, tooltip: { trigger: 'axis' }, grid: { left: 65, right: 20, bottom: 55, top: 40 },
    toolbox: { feature: { saveAsImage: { title: '保存图片', name: `模型对比-${chartIndustry.value}-${metric.value}` } } },
    xAxis: { type: 'category', data: data.map(row => modelDisplay(row.model)), axisLabel: { interval: 0,
      formatter: value => ({ '逻辑回归': '逻辑\n回归', '单棵决策树': '单棵\n决策树', '梯度提升树': '梯度\n提升树', 'TabPFN': 'TabPFN' }[value] || value) } },
    yAxis: { type: 'value', name: metrics.find(item => item.key === metric.value)?.short },
    series: [{ type: 'bar', barMaxWidth: 65, data: data.map((row, index) => ({ value: row[metric.value],
      itemStyle: { color: ['#3684b8', '#54a486', '#d7a34b', '#a276ad'][index] } })) }] }, true)
})

function exportCsv() {
  // 同时导出参数、划分与数据指纹，避免图表脱离实验背景；转义CSV公式与引号。
  const keys = ['industryName', 'model', 'mode', 'featureMode', 'trainYears', 'testYear', 'trainSamples', 'testSamples',
    ...metrics.map(item => item.key), 'fixedThreshold', 'selectedThreshold', 'thresholdSelection', 'probabilityRange', 'thresholdSeconds', 'parameters', 'tuning', 'dataFingerprint']
  const cell = value => {
    let text = typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? '')
    if (/^[=+@-]/.test(text)) text = "'" + text
    return '"' + text.replaceAll('"', '""') + '"'
  }
  const csv = [keys, ...rows.value.map(row => keys.map(key => row[key]))].map(row => row.map(cell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a'); link.href = url; link.download = `模型对比-${selectedId.value}.csv`; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
onMounted(async () => { await load(); if (!disposed) timer = setInterval(() => { if (busy.value) load() }, 2000) })
onUnmounted(() => { disposed = true; clearInterval(timer); observer?.disconnect(); chart?.dispose() })
</script>

<style scoped>
.comparison-page { padding: 20px; min-width: 0; }
.comparison-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.metric-help { max-height: 66vh; overflow-y: auto; padding-right: 12px; }
.metric-help h3 { font-size: 15px; margin: 16px 0 8px; color: #263c4a; }
.metric-definition { border-bottom: 1px solid #e5e9ec; padding-bottom: 8px; }
.metric-standard { color: #365975; }
.confusion-matrix { width: 100%; border-collapse: collapse; font-size: 13px; }
.confusion-matrix th, .confusion-matrix td { border: 1px solid #dce3e8; padding: 8px; text-align: center; }
.threshold-section h3 { font-size: 15px; margin-top: 20px; }
.comparison-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
h2 { font-size: 18px; margin: 0 20px 0 0; }
.comparison-results { background: white; padding: 20px; min-width: 0; }
p { font-size: 14px; color: #52616b; line-height: 1.7; overflow-wrap: anywhere; }
.chart-toolbar { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px; }
.comparison-chart { width: 100%; height: 340px; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 320px; overflow: auto; }
</style>
