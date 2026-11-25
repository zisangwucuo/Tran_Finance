<template>
  <div
    v-if="visible"
    class="trend-hover-preview"
    :style="panelStyle"
    @mouseenter="emit('mouseenter')"
    @mouseleave="emit('mouseleave')"
  >
    <div class="preview-header">
      <strong>{{ companyCode || '当前企业' }}</strong>
      <span>近三年转型综合评分</span>
    </div>
    <div v-if="loading" class="preview-empty">正在加载趋势数据...</div>
    <div v-else-if="loadFailed" class="preview-empty preview-error">趋势数据加载失败</div>
    <div v-else-if="trendRows.length === 0" class="preview-empty">暂无近三年评分数据</div>
    <div v-else ref="chartRef" class="preview-chart"></div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { fetchTransitionScoreTrend } from '../api/electricCompanyData'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  companyCode: {
    type: String,
    default: ''
  },
  institutionId: {
    type: String,
    default: ''
  },
  industryKey: {
    type: String,
    default: ''
  },
  x: {
    type: Number,
    default: 0
  },
  y: {
    type: Number,
    default: 0
  }
})
const emit = defineEmits(['mouseenter', 'mouseleave'])

const chartRef = ref()
const loading = ref(false)
const loadFailed = ref(false)
const trendRows = ref([])
let chartInstance = null
let requestNo = 0

const panelStyle = computed(() => {
  const panelWidth = 420
  const panelHeight = 300
  const viewportWidth = typeof window === 'undefined' ? panelWidth + 32 : window.innerWidth
  const viewportHeight = typeof window === 'undefined' ? panelHeight + 32 : window.innerHeight
  const left = Math.max(16, Math.min(props.x + 14, viewportWidth - panelWidth - 16))
  const top = Math.max(16, Math.min(props.y + 14, viewportHeight - panelHeight - 16))
  return {
    left: `${left}px`,
    top: `${top}px`
  }
})

watch(
  () => [props.visible, props.companyCode, props.institutionId, props.industryKey],
  async () => {
    if (!props.visible) {
      disposeChart()
      return
    }
    await loadTrendRows()
  },
  { flush: 'post' }
)

onBeforeUnmount(() => {
  disposeChart()
})

async function loadTrendRows() {
  const currentRequestNo = ++requestNo
  disposeChart()
  trendRows.value = []
  loadFailed.value = false
  if (!props.companyCode || !props.institutionId || !props.industryKey) {
    return
  }
  loading.value = true
  try {
    const rows = await fetchTransitionScoreTrend({
      institutionId: props.institutionId,
      industryKey: props.industryKey,
      companyCode: props.companyCode
    })
    if (currentRequestNo !== requestNo) {
      return
    }
    trendRows.value = rows || []
    loading.value = false
    await renderChart()
  } catch {
    if (currentRequestNo === requestNo) {
      loadFailed.value = true
      trendRows.value = []
    }
  } finally {
    if (currentRequestNo === requestNo) {
      loading.value = false
    }
  }
}

async function renderChart() {
  await nextTick()
  await waitForNextFrame()
  disposeChart()
  if (!chartRef.value || trendRows.value.length === 0) {
    return
  }
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    grid: { left: 42, right: 18, top: 28, bottom: 34 },
    tooltip: {
      trigger: 'axis',
      appendToBody: true,
      formatter: (params) => {
        const item = params?.[0]
        return item ? `${item.axisValue}年：${item.data}分` : ''
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendRows.value.map((item) => `${item.dataYear}`),
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { color: '#64748b', formatter: '{value}' },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [
      {
        name: '转型综合评分',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: trendRows.value.map((item) => Number(item.transitionScore ?? 0)),
        areaStyle: { color: 'rgba(37, 99, 235, 0.12)' },
        lineStyle: { color: '#2563eb', width: 3 },
        itemStyle: { color: '#2563eb' }
      }
    ]
  })
  chartInstance.resize()
}

function disposeChart() {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

function waitForNextFrame() {
  return new Promise((resolve) => {
    window.requestAnimationFrame(resolve)
  })
}
</script>

<style scoped>
.trend-hover-preview {
  position: fixed;
  z-index: 3200;
  width: 420px;
  max-width: calc(100vw - 32px);
  padding: 14px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.2);
}

.preview-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-header strong {
  color: #1f2a44;
  font-size: 15px;
}

.preview-header span {
  color: #64748b;
  font-size: 12px;
}

.preview-chart {
  height: 235px;
}

.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 235px;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #dbe3ef;
  border-radius: 6px;
  font-size: 13px;
}

.preview-error {
  color: #b42318;
  background: #fff6f4;
  border-color: #ffd8d2;
}
</style>
