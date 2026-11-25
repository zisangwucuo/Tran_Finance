<template>
  <el-container class="dashboard-shell">
    <el-aside class="sidebar" width="210px">
      <div class="sidebar-logo">企业系统</div>
      <el-menu
        active-text-color="#ffffff"
        background-color="#0f2742"
        class="side-menu"
        :default-active="route.path"
        router
        text-color="#c5d2df"
      >
        <el-menu-item index="/company/dashboard/history-data">
          <el-icon><DataAnalysis /></el-icon>
          <span>已评估历史数据</span>
        </el-menu-item>
        <el-menu-item index="/company/dashboard/pending-data">
          <el-icon><Clock /></el-icon>
          <span>待评估数据</span>
        </el-menu-item>
        <el-menu-item index="/company/dashboard/missing-data">
          <el-icon><Tickets /></el-icon>
          <span>待补全数据</span>
        </el-menu-item>
        <el-menu-item index="/company/dashboard/materials">
          <el-icon><DocumentChecked /></el-icon><span>材料上传与指标提取</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button :icon="Fold" circle text />
          <span>企业端</span>
        </div>
        <div class="topbar-right">
          <el-button :icon="Refresh" circle text @click="reload" />
          <el-dropdown trigger="click">
            <span class="avatar-entry">
              <el-avatar :icon="UserFilled" :size="32" />
              <span>{{ user?.companyCode || '企业用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openProfileDialog">修改企业资料</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-area">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="profileVisible" title="企业基础信息" width="720px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="96px">
        <div class="profile-grid">
          <el-form-item label="企业编码">
            <el-input v-model="profileForm.companyCode" disabled />
          </el-form-item>
          <el-form-item label="股票代码">
            <el-input v-model="profileForm.stockCode" disabled />
          </el-form-item>
          <el-form-item label="企业类型" prop="companyType">
            <el-select v-model="profileForm.companyType">
              <el-option label="上市企业" value="上市企业" />
              <el-option label="非上市企业" value="非上市企业" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属行业" prop="industryKey">
            <el-select v-model="profileForm.industryKey">
              <el-option v-for="item in industryOptions" :key="item.key" :label="item.name" :value="item.key" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model.trim="profileForm.contactName" />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model.trim="profileForm.contactPhone" />
          </el-form-item>
          <el-form-item class="full-row" label="地址">
            <el-input v-model.trim="profileForm.address" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button :loading="savingProfile" type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, DataAnalysis, Fold, Refresh, Tickets, UserFilled, DocumentChecked } from '@element-plus/icons-vue'
import request from '../api/request'
import { fetchCompanyProfile, updateCompanyProfile } from '../api/companyAuth'
import { industryOptions } from '../utils/industryOptions'

const router = useRouter()
const route = useRoute()
const profileVisible = ref(false)
const savingProfile = ref(false)
const profileFormRef = ref()
const profileForm = reactive({})

const user = computed(() => {
  const raw = localStorage.getItem('companyUser')
  return raw ? JSON.parse(raw) : null
})

const profileRules = {
  companyType: [{ required: true, message: '请选择企业类型', trigger: 'change' }],
  industryKey: [{ required: true, message: '请选择所属行业', trigger: 'change' }]
}

function reload() {
  window.location.reload()
}

async function logout() {
  try { await request.post('/company-auth/logout') } catch { return }
  localStorage.removeItem('companyUser')
  router.push('/company/login')
}

async function openProfileDialog() {
  const profile = await fetchCompanyProfile(user.value.companyCode)
  Object.keys(profileForm).forEach((key) => delete profileForm[key])
  Object.assign(profileForm, profile)
  profileVisible.value = true
}

async function saveProfile() {
  await profileFormRef.value.validate()
  savingProfile.value = true
  try {
    const profile = await updateCompanyProfile(profileForm)
    const nextUser = { ...user.value, companyName: profile.companyName, industryKey: profile.industryKey, industryName: profile.industryName }
    localStorage.setItem('companyUser', JSON.stringify(nextUser))
    ElMessage.success('企业资料已更新')
    profileVisible.value = false
  } finally {
    savingProfile.value = false
  }
}
</script>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
}

.sidebar {
  color: #fff;
  background: #0f2742;
}

.sidebar-logo {
  height: 56px;
  display: grid;
  place-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 18px;
  font-weight: 800;
}

.side-menu {
  border-right: none;
}

.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.topbar-left,
.topbar-right,
.avatar-entry {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-entry {
  cursor: pointer;
}

.main-area {
  padding: 0;
  background: #eef2f7;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.profile-grid :deep(.el-select) {
  width: 100%;
}

.full-row {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .sidebar {
    display: none;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
