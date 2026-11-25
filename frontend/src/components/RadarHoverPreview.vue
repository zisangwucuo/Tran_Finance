<template>
  <div
    v-if="visible"
    class="radar-hover-preview"
    :style="panelStyle"
    @mouseenter="emit('mouseenter')"
    @mouseleave="emit('mouseleave')"
  >
    <div class="preview-header">
      <strong>{{ titleText }}</strong>
      <span>指标雷达图</span>
    </div>
    <div class="preview-grid">
      <section class="preview-panel">
        <h3>通用指标雷达图</h3>
        <div ref="commonChartRef" class="preview-chart"></div>
      </section>
      <section class="preview-panel">
        <h3>专项指标雷达图</h3>
        <div ref="specialtyChartRef" class="preview-chart"></div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  comprehensiveIndicators,
  specialtyIndicators
} from '../utils/radarConfig'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  row: {
    type: Object,
    default: null
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

const commonChartRef = ref()
const specialtyChartRef = ref()
let chartInstances = []

const titleText = computed(() => {
  return props.row?.companyCode || '未关联企业'
})

const panelStyle = computed(() => {
  const panelWidth = 720
  const panelHeight = 390
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
  () => [props.visible, props.row, props.industryKey],
  async () => {
    if (props.visible) {
      await renderCharts()
    } else {
      disposeCharts()
    }
  },
  { flush: 'post' }
)

onBeforeUnmount(() => {
  disposeCharts()
})

async function renderCharts() {
  await nextTick()
  disposeCharts()
  if (!props.row) {
    return
  }
  chartInstances = [
    renderRadarChart(commonChartRef.value, '通用指标', buildRadarItems(comprehensiveIndicators, props.row)),
    renderRadarChart(specialtyChartRef.value, '专项指标', buildRadarItems(specialtyIndicators[props.industryKey] || [], props.row))
  ].filter(Boolean)
}

function renderRadarChart(target, title, items) {
  if (!target || items.length === 0) {
    return null
  }
  const chart = echarts.init(target)
  chart.setOption({
    tooltip: {
      appendToBody: true,
      formatter: () => items.map((item) => `${item.label}: ${formatTableCell(item.raw)}（${item.score}分）`).join('<br/>')
    },
    radar: {
      radius: '60%',
      indicator: items.map((item) => ({ name: item.label, max: 100 })),
      axisName: { color: '#475569', fontSize: 10 }
    },
    series: [
      {
        name: title,
        type: 'radar',
        areaStyle: { color: 'rgba(37, 99, 235, 0.16)' },
        lineStyle: { color: '#2563eb', width: 2 },
        itemStyle: { color: '#2563eb' },
        data: [{ value: items.map((item) => item.score), name: titleText.value }]
      }
    ]
  })
  return chart
}

function disposeCharts() {
  chartInstances.forEach((chart) => chart.dispose())
  chartInstances = []
}

function buildRadarItems(indicators, row) {
  return indicators.map((indicator) => {
    const raw = row[indicator.prop]
    return {
      ...indicator,
      raw,
      score: scoreIndicator(indicator, raw)
    }
  })
}

function scoreIndicator(indicator, raw) {
  if (raw === null || raw === undefined || raw === '') {
    return 0
  }
  if (indicator.type === 'boolean') {
    return Number(raw) === 1 ? 100 : 0
  }
  if (indicator.type === 'rating') {
    return scoreRating(raw)
  }
  if (indicator.type === 'score') {
    const numeric = Number(raw)
    return Number.isNaN(numeric) ? 0 : clamp(numeric > 1 ? numeric : numeric * 100)
  }
  const numeric = Number(raw)
  if (Number.isNaN(numeric)) {
    return 0
  }
  if (indicator.direction === 'lower') {
    return clamp(((indicator.lag - numeric) / (indicator.lag - indicator.best)) * 100)
  }
  return clamp(((numeric - indicator.lag) / (indicator.best - indicator.lag)) * 100)
}

function scoreRating(value) {
  if (String(value).includes('A')) {
    return 100
  }
  if (String(value).includes('B')) {
    return 70
  }
  if (String(value).includes('C')) {
    return 35
  }
  return 0
}

function clamp(value) {
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(0, Math.min(100, Number(value.toFixed(1))))
}

function formatTableCell(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}
</script>

<style scoped>
.radar-hover-preview {
  position: fixed;
  z-index: 3200;
  width: 720px;
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

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preview-panel {
  min-width: 0;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.preview-panel h3 {
  margin: 0 0 4px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.preview-chart {
  height: 285px;
}

@media (max-width: 760px) {
  .radar-hover-preview {
    width: calc(100vw - 32px);
  }

  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
