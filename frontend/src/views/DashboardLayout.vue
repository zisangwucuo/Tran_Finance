<template>
  <el-config-provider :locale="zhCn">
  <el-container class="dashboard-shell financial-shell">
    <el-aside v-if="!isMobile" class="sidebar" :width="collapsed ? '76px' : '232px'">
      <div class="sidebar-logo">
        <el-icon class="brand-icon"><DataLine /></el-icon>
        <div v-if="!collapsed"><strong>转型金融</strong><span>金融机构工作台</span></div>
      </div>
      <nav aria-label="金融机构业务导航"><FinancialNavigation :collapsed="collapsed" /></nav>
      <div class="sidebar-institution" :title="institutionName">
        <el-icon><OfficeBuilding /></el-icon>
        <div v-if="!collapsed"><strong>{{ institutionName }}</strong><span>{{ user?.institutionId || '金融机构' }}</span></div>
      </div>
    </el-aside>

    <el-container class="workspace-shell">
      <el-header class="topbar">
        <div class="topbar-left">
          <el-tooltip :content="isMobile ? '打开导航' : collapsed ? '展开导航' : '收起导航'">
            <el-button :icon="collapsed || isMobile ? Expand : Fold" circle text
              :aria-label="isMobile ? '打开导航' : collapsed ? '展开导航' : '收起导航'" @click="toggleNavigation" />
          </el-tooltip>
          <div class="workspace-breadcrumb" aria-label="当前位置"><span>{{ currentPage.group }}</span><el-icon><ArrowRight /></el-icon><strong>{{ currentPage.label }}</strong></div>
        </div>
        <div class="topbar-right">
          <el-tooltip content="刷新当前页面"><el-button :icon="Refresh" aria-label="刷新当前页面" circle text @click="reload" /></el-tooltip>
          <el-avatar :icon="UserFilled" :size="32" />
          <div class="user-block">
            <strong>{{ user?.username || '金融机构用户' }}</strong>
            <span>{{ institutionName }}</span>
          </div>
          <el-button :icon="SwitchButton" text type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-area">
        <router-view />
      </el-main>
    </el-container>
    <el-drawer v-model="mobileNavigationVisible" title="转型金融 · 机构工作台" direction="ltr" size="min(280px, 90vw)" class="financial-navigation-drawer">
      <nav aria-label="金融机构业务导航"><FinancialNavigation @navigate="mobileNavigationVisible = false" /></nav>
    </el-drawer>
  </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ArrowRight, DataLine, Expand, Fold, OfficeBuilding, Refresh, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import request from '../api/request'
import FinancialNavigation from '../components/FinancialNavigation.vue'
import { financialPage } from '../utils/financialNavigation'
import '../styles/financial-workspace.css'

const router = useRouter()
const route = useRoute()
const currentPage = computed(() => financialPage(route.path))
const institutionName = computed(() => user.value?.institutionName || user.value?.institutionId || '未绑定机构')
const collapsed = ref(localStorage.getItem('financialSidebarCollapsed') === 'true')
const mobileNavigationVisible = ref(false)
const mobileQuery = window.matchMedia('(max-width: 900px)')
const isMobile = ref(mobileQuery.matches)

// 桌面折叠与手机抽屉共享菜单；切换屏幕尺寸时解除抽屉遮罩。
function updateViewport(event) {
  isMobile.value = event.matches
  mobileNavigationVisible.value = false
}
function toggleNavigation() {
  if (isMobile.value) mobileNavigationVisible.value = true
  else {
    collapsed.value = !collapsed.value
    localStorage.setItem('financialSidebarCollapsed', String(collapsed.value))
  }
}
onMounted(() => mobileQuery.addEventListener('change', updateViewport))
onUnmounted(() => mobileQuery.removeEventListener('change', updateViewport))

// 用户信息来自登录响应，仅包含用户名与机构信息。
const user = computed(() => {
  const raw = localStorage.getItem('financialUser')
  return raw ? JSON.parse(raw) : null
})

function reload() {
  window.location.reload()
}

async function logout() {
  try { await request.post('/financial-auth/logout') } catch { return }
  localStorage.removeItem('financialUser')
  router.push('/login')
}
</script>
