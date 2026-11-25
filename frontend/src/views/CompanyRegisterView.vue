<template>
  <main class="company-auth-shell">
    <section class="auth-panel">
      <div class="brand-block">
        <div class="brand-mark">CE</div>
        <h1>企业账号注册</h1>
        <p>企业编码将在注册成功后由系统自动生成</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleRegister">
        <div class="register-grid">
          <el-form-item label="企业名称" prop="companyName">
            <el-input v-model.trim="form.companyName" type="password" autocomplete="off" size="large" />
          </el-form-item>
          <el-form-item label="企业类型" prop="companyType">
            <el-select v-model="form.companyType" size="large" @change="handleCompanyTypeChange">
              <el-option label="上市企业" value="上市企业" />
              <el-option label="非上市企业" value="非上市企业" />
            </el-select>
          </el-form-item>
          <el-form-item label="股票代码" prop="stockCode">
            <el-input v-model.trim="form.stockCode" :disabled="form.companyType === '非上市企业'" size="large" />
          </el-form-item>
          <el-form-item label="所属行业" prop="industryKey">
            <el-select v-model="form.industryKey" size="large">
              <el-option v-for="item in industryOptions" :key="item.key" :label="item.name" :value="item.key" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model.trim="form.contactName" size="large" />
          </el-form-item>
          <el-form-item label="联系方式" prop="contactPhone">
            <el-input v-model.trim="form.contactPhone" size="large" />
          </el-form-item>
          <el-form-item class="full-row" label="地址" prop="address">
            <el-input v-model.trim="form.address" size="large" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="form.username" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" show-password size="large" type="password" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" show-password size="large" type="password" />
          </el-form-item>
        </div>
        <el-button :loading="loading" class="submit-button" size="large" type="primary" @click="handleRegister">
          注册
        </el-button>
      </el-form>

      <div class="auth-link">
        <router-link to="/company/login">返回企业登录</router-link>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { companyRegister } from '../api/companyAuth'
import { industryOptions } from '../utils/industryOptions'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  companyName: '',
  companyType: '上市企业',
  stockCode: '',
  industryKey: 'power',
  contactName: '',
  contactPhone: '',
  address: '',
  username: '',
  password: '',
  confirmPassword: ''
})

function validateConfirmPassword(rule, value, callback) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = computed(() => ({
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  companyType: [{ required: true, message: '请选择企业类型', trigger: 'change' }],
  stockCode: form.companyType === '上市企业' ? [{ required: true, message: '上市企业必须填写股票代码', trigger: 'blur' }] : [],
  industryKey: [{ required: true, message: '请选择所属行业', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}))

function handleCompanyTypeChange() {
  if (form.companyType === '非上市企业') {
    form.stockCode = ''
  }
  formRef.value?.clearValidate('stockCode')
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const user = await companyRegister(form)
    localStorage.setItem('companyUser', JSON.stringify(user))
    ElMessage.success(`注册成功，企业编码：${user.companyCode}`)
    router.push('/company/dashboard/history-data')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.company-auth-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(rgba(12, 41, 58, 0.78), rgba(17, 56, 64, 0.72)),
    url("https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1800&q=80") center/cover;
}

.auth-panel {
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  padding: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.24);
}

.brand-block {
  margin-bottom: 24px;
  text-align: center;
}

.brand-mark {
  width: 52px;
  height: 52px;
  display: inline-grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  font-weight: 800;
  background: #2563eb;
}

.brand-block h1 {
  margin: 16px 0 8px;
  font-size: 24px;
  letter-spacing: 0;
}

.brand-block p {
  margin: 0;
  color: #64748b;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.full-row {
  grid-column: 1 / -1;
}

.register-grid :deep(.el-select) {
  width: 100%;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
}

.auth-link {
  margin-top: 18px;
  text-align: center;
}

@media (max-width: 720px) {
  .register-grid {
    grid-template-columns: 1fr;
  }
}
</style>
