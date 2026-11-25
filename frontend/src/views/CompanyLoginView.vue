<template>
  <LoginLayout portal="company">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk @submit.prevent="handleLogin">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" placeholder="请输入企业用户名" :prefix-icon="User" name="username" autocomplete="username" size="large" />
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
import { companyLogin } from '../api/companyAuth'
import LoginLayout from '../components/LoginLayout.vue'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  // 通过原生表单提交统一支持回车和按钮，并避免请求期间再次提交。
  if (loading.value) return
  loading.value = true
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    const user = await companyLogin(form)
    localStorage.setItem('companyUser', JSON.stringify(user))
    ElMessage.success('登录成功')
    await router.push('/company/dashboard/history-data')
  } catch {
    // 统一请求层负责错误提示，页面接住异常后恢复可重试状态。
  } finally {
    loading.value = false
  }
}
</script>
