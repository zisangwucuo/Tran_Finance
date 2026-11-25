<template>
  <main class="auth-shell">
    <section class="auth-panel">
      <div class="brand-block">
        <div class="brand-mark">TF</div>
        <h1>金融机构用户注册</h1>
        <p>填写机构资料并创建登录账号</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleRegister">
        <div class="section-title">机构信息</div>
        <div class="register-grid">
          <el-form-item label="金融机构名称" prop="institutionName">
            <el-input v-model.trim="form.institutionName" placeholder="请输入金融机构名称" size="large" />
          </el-form-item>
          <el-form-item label="金融机构类型" prop="institutionType">
            <el-select v-model="form.institutionType" placeholder="请选择机构类型" size="large">
              <el-option label="银行" value="bank" />
              <el-option label="担保机构" value="guarantee" />
              <el-option label="其他金融机构" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model.trim="form.contactName" placeholder="请输入联系人" size="large" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model.trim="form.contactPhone" placeholder="请输入联系电话" size="large" />
          </el-form-item>
        </div>

        <div class="section-title">账号信息</div>
        <div class="register-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="form.username" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" placeholder="请输入密码" show-password size="large" type="password" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" placeholder="请再次输入密码" show-password size="large" type="password" />
          </el-form-item>
        </div>

        <el-button :loading="loading" class="submit-button" size="large" type="primary" @click="handleRegister">
          注册
        </el-button>
      </el-form>

      <div class="auth-link">
        <span>已有账号</span>
        <router-link to="/login">返回登录</router-link>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

// 注册时由后端生成不重复的金融机构编码，前端只提交机构主数据和账号资料。
const form = reactive({
  institutionName: '',
  institutionType: 'bank',
  contactName: '',
  contactPhone: '',
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

const rules = {
  institutionName: [
    { required: true, message: '请输入金融机构名称', trigger: 'blur' },
    { max: 200, message: '长度不能超过200个字符', trigger: 'blur' }
  ],
  institutionType: [{ required: true, message: '请选择金融机构类型', trigger: 'change' }],
  contactName: [{ max: 100, message: '长度不能超过100个字符', trigger: 'blur' }],
  contactPhone: [
    { max: 30, message: '长度不能超过30个字符', trigger: 'blur' },
    { pattern: /^[0-9+\-\s()]*$/, message: '联系电话格式不正确', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '长度必须在3到64个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '长度必须在6到64个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const user = await register(form)
    localStorage.setItem('financialUser', JSON.stringify(user))
    ElMessage.success(`注册成功，机构编码：${user.institutionId}`)
    router.push('/dashboard/customer-overview')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(rgba(7, 24, 44, 0.82), rgba(7, 24, 44, 0.7)),
    url("https://images.unsplash.com/photo-1554224155-6726b3ff858f?auto=format&fit=crop&w=1800&q=80") center/cover;
}

.auth-panel {
  width: min(620px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  padding: 32px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
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
  background: #0f766e;
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

.section-title {
  margin: 18px 0 12px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.register-grid :deep(.el-select) {
  width: 100%;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
}

.auth-link {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
  font-size: 14px;
}

.auth-link a {
  color: #0f766e;
  text-decoration: none;
}

@media (max-width: 720px) {
  .auth-shell {
    padding: 16px;
  }

  .auth-panel {
    padding: 24px;
  }

  .register-grid {
    grid-template-columns: 1fr;
  }
}
</style>
