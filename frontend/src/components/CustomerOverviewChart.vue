<template>
  <div ref="element" class="overview-chart" role="img" :aria-label="description" />
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ option: { type: Object, required: true }, description: { type: String, required: true } })
const element = ref()
let chart, observer, resizeFrame

// 容器观察器同时覆盖窗口缩放和侧栏折叠；离开页面后释放画布及观察器。
onMounted(() => {
  chart = echarts.init(element.value)
  chart.setOption(props.option)
  observer = new ResizeObserver(() => {
    cancelAnimationFrame(resizeFrame)
    resizeFrame = requestAnimationFrame(() => chart?.resize())
  })
  observer.observe(element.value)
})
watch(() => props.option, option => chart?.setOption(option, true))
onBeforeUnmount(() => {
  cancelAnimationFrame(resizeFrame)
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.overview-chart { width: 100%; height: 310px; min-width: 0; }
</style>
