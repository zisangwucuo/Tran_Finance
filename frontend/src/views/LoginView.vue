<template>
  <LoginLayout portal="financial">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk @submit.prevent="handleLogin">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" placeholder="请输入金融机构用户名" :prefix-icon="User" name="username" autocomplete="username" size="large" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" placeholder="请输入密码" :prefix-icon="Lock" name="password" autocomplete="current-password" show-password size="large" type="password" />
      </el-form-item>
      <el-button :loading="loading" :disabled="loading" class="submit-button" size="large" type="primary" native-type="submit">
        {{ loading ? '正在登录' : '登录工作台' }}<el-icon v-if="!loading" class="submit-arrow"><Right /></el-icon>
      </el-button>
    </el-form>
  </LoginLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Right, User } from '@element-plus/icons-vue'
import { login } from '../api/auth'
import LoginLayout from '../components/LoginLayout.vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

// 登录表单只在提交时传输密码，成功后本地只保存非敏感用户信息。
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  // 提交前锁定状态，避免回车与鼠标连点产生重复登录请求。
  if (loading.value) return
  loading.value = true
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    const user = await login(form)
    localStorage.setItem('financialUser', JSON.stringify(user))
    ElMessage.success('登录成功')
    await router.push('/dashboard/customer-overview')
  } catch {
    // 请求拦截器已展示服务错误；在此接住拒绝，保留表单供用户修正后重试。
  } finally {
    loading.value = false
  }
}
</script>
