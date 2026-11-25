<template>
  <el-menu :default-active="route.path" :collapse="collapsed" :collapse-transition="false"
    class="financial-menu" router @select="emit('navigate')">
    <el-menu-item-group v-for="group in financialNavigation" :key="group.title">
      <template #title>
        <span v-if="!collapsed" class="nav-group-title"><span>{{ group.title }}</span><small>{{ group.number }}</small></span>
      </template>
      <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path" :aria-label="item.label"
        :aria-current="route.path === item.path ? 'page' : undefined">
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title><span>{{ item.label }}</span></template>
      </el-menu-item>
    </el-menu-item-group>
  </el-menu>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { financialNavigation } from '../utils/financialNavigation'
defineProps({ collapsed: Boolean })
const emit = defineEmits(['navigate'])
const route = useRoute()
</script>
