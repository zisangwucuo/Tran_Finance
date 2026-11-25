<template>
  <dl class="financial-data-summary" aria-label="当前数据概览" :aria-busy="loading">
    <div v-for="item in items" :key="item.label" :class="item.tone">
      <dt>{{ item.label }}</dt>
      <dd>{{ loading ? '-' : item.value }}<small v-if="item.unit && !loading">{{ item.unit }}</small></dd>
    </div>
  </dl>
</template>

<script setup>
// 仅展示页面已有响应的统计，不额外查询或将本页统计冒充全量统计。
defineProps({ items: { type: Array, required: true }, loading: Boolean })
</script>
