<template>
  <div class="login-page">
    <header class="login-header">
      <router-link class="login-brand" :to="loginPath" aria-label="转型金融登录首页">
        <el-icon class="login-brand-icon"><DataLine /></el-icon>
        <span>转型金融</span>
      </router-link>
      <span class="login-header-label">{{ workspaceName }}</span>
    </header>

    <main class="login-main">
      <section class="login-visual" aria-labelledby="platform-title">
        <div class="login-visual-heading">
          <p class="login-eyebrow">TRANSITION FINANCE</p>
          <h1 id="platform-title"><span>企业转型金融</span><span>数据平台</span></h1>
          <p class="login-visual-caption">让金融与绿色转型同行</p>
        </div>
        <!-- 插画随前端打包，本地加载；不使用真实客户数据或外部图片服务。 -->
        <img class="login-illustration" :src="financeScene" width="1400" height="1000"
          alt="银行与金融建筑、光伏板、风力发电机和绿植组成的绿色金融场景" fetchpriority="high" />
        <div class="login-visual-footer"><span>绿色转型</span><i aria-hidden="true"></i><span>可持续金融</span></div>
      </section>

      <section class="login-panel" aria-labelledby="login-title">
        <div class="login-form-content">
          <div class="login-panel-heading">
            <el-icon class="login-institution-icon"><OfficeBuilding /></el-icon>
            <h2 id="login-title">{{ portal === 'company' ? '企业账号登录' : '金融机构登录' }}</h2>
            <p>欢迎回到转型金融工作台</p>
          </div>

          <slot />

          <div class="login-registration">
            <span>还没有账号？</span>
            <router-link :to="registerPath">{{ registerLabel }}<el-icon><ArrowRight /></el-icon></router-link>
          </div>
        </div>
      </section>
    </main>

    <footer class="login-footer">
      <span>企业转型金融数据平台</span><span class="login-footer-divider" aria-hidden="true"></span><span>{{ workspaceName }}</span>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowRight, DataLine, OfficeBuilding } from '@element-plus/icons-vue'
import financeScene from '../assets/green-finance-scene.png'

// 默认仅呈现金融机构入口，暂时移除企业端切换；保留旧页面复用能力与认证逻辑。
const props = defineProps({
  portal: { type: String, default: 'financial', validator: (value) => ['financial', 'company'].includes(value) }
})
const workspaceName = computed(() => props.portal === 'company' ? '企业工作台' : '金融机构工作台')
const loginPath = computed(() => props.portal === 'company' ? '/company/login' : '/login')
const registerPath = computed(() => props.portal === 'company' ? '/company/register' : '/register')
const registerLabel = computed(() => props.portal === 'company' ? '注册企业账号' : '注册金融机构账号')
</script>

<style scoped>
/* 主题仅在登录布局内生效，沿用机构工作台的主色、边框及文字层级。 */
.login-page {
  --el-color-primary: #177b78;
  --el-color-primary-light-3: #589f9d;
  --el-color-primary-light-5: #8bbdba;
  --el-color-primary-light-7: #bddad7;
  --el-color-primary-light-8: #d9ece9;
  --el-color-primary-light-9: #edf7f5;
  --el-color-primary-dark-2: #12635f;
  --el-text-color-primary: #263a40;
  --el-border-color: #dce4e6;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  color: #263a40;
  letter-spacing: 0;
  background: #edf2f1;
}
.login-header {
  min-height: 76px;
  padding: 16px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: #f8fbf9;
  border-bottom: 1px solid #dee7e2;
}
.login-brand { display: inline-flex; align-items: center; gap: 12px; color: #204941; text-decoration: none; font-size: 20px; font-weight: 700; }
.login-brand-icon { width: 36px; height: 36px; flex-shrink: 0; border-radius: 8px; color: #ffffff; background: #177b78; font-size: 23px; }
.login-header-label { font-size: 13px; color: #6c7e82; }
.login-main {
  width: min(1440px, calc(100% - 96px));
  flex: 1;
  min-height: 640px;
  display: grid;
  grid-template-columns: 1.12fr 1fr;
  margin: 32px auto 20px;
  border: 1px solid #d6e3db;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 16px 48px #294c420c;
}
.login-visual { min-width: 0; padding: 44px 44px 30px; display: flex; flex-direction: column; background: linear-gradient(145deg, #dcece1, #e9f3ec 55%, #d8ebe5); }
.login-visual-heading { position: relative; z-index: 1; }
.login-eyebrow { margin: 0 0 18px; color: #577e6c; font-size: 12px; font-weight: 600; }
.login-visual h1 { margin: 0; color: #214c3c; font-size: 38px; line-height: 1.4; font-weight: 600; overflow-wrap: anywhere; }
.login-visual h1 span { display: block; }
.login-visual-caption { margin: 18px 0 0; color: #5f7d6e; font-size: 15px; }
.login-illustration { width: calc(100% + 32px); max-height: 420px; height: auto; aspect-ratio: 7 / 5; object-fit: contain; display: block; margin: auto -16px 0; }
.login-visual-footer { display: flex; align-items: center; justify-content: center; gap: 18px; color: #658577; font-size: 12px; margin-top: 12px; }
.login-visual-footer i { display: block; width: 24px; height: 1px; background: #a8c3b2; }
.login-panel { min-width: 0; padding: 52px 56px; display: flex; align-items: center; justify-content: center; border-left: 1px solid #dce8df; }
.login-form-content { width: 100%; max-width: 400px; }
.login-panel-heading { margin-bottom: 36px; }
.login-institution-icon { width: 48px; height: 48px; margin-bottom: 22px; background: #e9f4ef; color: #177b78; border-radius: 8px; font-size: 26px; }
.login-panel-heading h2 { margin: 0; color: #29443e; font-size: 28px; font-weight: 600; }
.login-panel-heading p { margin: 12px 0 0; color: #7b8b8d; font-size: 14px; }
/* 表单保持真实提交能力；预留校验错误空间，避免错误与下一项输入重叠。 */
.login-panel :deep(.el-form-item) { margin-bottom: 28px; }
.login-panel :deep(.el-form-item__label) { height: auto; line-height: 20px; margin-bottom: 10px; padding: 0; color: #4a625e; font-size: 14px; }
.login-panel :deep(.el-input__wrapper) { min-height: 50px; padding: 0 14px; border-radius: 5px; box-shadow: 0 0 0 1px #dce5e3 inset; transition: box-shadow 150ms; }
.login-panel :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #9fbfba inset; }
.login-panel :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #177b78 inset, 0 0 0 3px #177b7810; }
.login-panel :deep(.is-error .el-input__wrapper) { box-shadow: 0 0 0 1px var(--el-color-danger) inset; }
.login-panel :deep(.el-input__prefix) { margin-right: 6px; color: #8aa09a; font-size: 17px; }
.login-panel :deep(.el-input__inner) { color: #304b45; font-size: 14px; }
.login-panel :deep(.el-input__inner::placeholder) { color: #98a6a3; }
.login-panel :deep(.submit-button) { width: 100%; min-height: 50px; margin-top: 8px; border-radius: 5px; font-size: 15px; font-weight: 600; }
.login-panel :deep(.submit-arrow) { margin-left: 10px; }
.login-registration { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 6px; border-top: 1px solid #edf1f0; margin-top: 32px; padding-top: 26px; font-size: 13px; }
.login-registration > span { color: #7c8c87; }
.login-registration a { display: inline-flex; align-items: center; gap: 4px; color: #177b78; text-decoration: none; line-height: 24px; }
.login-registration a:hover { color: #12635f; text-decoration: underline; }
.login-footer { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 12px; padding: 8px 24px 22px; color: #7d8d8f; font-size: 12px; line-height: 20px; }
.login-footer-divider { width: 1px; height: 12px; background: #cbd8d5; }
.login-page :is(a, button):focus-visible { outline: 2px solid #177b78; outline-offset: 3px; }
@media (min-width: 1600px) {
  .login-main { flex: none; min-height: 740px; margin-top: auto; margin-bottom: auto; }
  .login-visual { padding: 52px 56px 34px; }
  .login-illustration { max-height: 470px; }
  .login-footer { padding-top: 22px; }
}
@media (max-width: 1100px), (max-height: 800px) {
  .login-main { min-height: 570px; width: calc(100% - 64px); margin-top: 24px; }
  .login-visual { padding: 34px 32px 24px; }
  .login-visual h1 { font-size: 32px; }
  .login-illustration { max-height: 340px; }
  .login-panel { padding: 40px; }
}
/* 手机与窄窗口转为上下结构；视觉元素缩成顶部横幅，表单保持足够的点击面积。 */
@media (max-width: 760px) {
  .login-header { min-height: 64px; padding: 12px 20px; gap: 12px; }
  .login-brand { font-size: 18px; gap: 9px; }
  .login-brand-icon { width: 32px; height: 32px; font-size: 21px; }
  .login-header-label { font-size: 12px; }
  .login-main { flex: none; width: calc(100% - 32px); min-height: 0; grid-template-columns: minmax(0, 1fr); margin: 20px auto; }
  .login-visual { display: grid; grid-template-columns: minmax(0, 1fr) 136px; align-items: center; gap: 4px; padding: 24px; }
  .login-eyebrow { font-size: 10px; margin-bottom: 10px; }
  .login-visual h1 { font-size: 23px; }
  .login-visual-caption, .login-visual-footer { display: none; }
  .login-illustration { width: 152px; height: 130px; max-height: none; margin: 0 -12px; }
  .login-panel { padding: 28px 24px; border-left: 0; border-top: 1px solid #dce8df; }
  .login-institution-icon { display: none; }
  .login-panel-heading { margin-bottom: 26px; }
  .login-panel-heading h2 { font-size: 23px; }
  .login-panel-heading p { font-size: 13px; margin-top: 8px; }
  .login-registration { margin-top: 24px; padding-top: 20px; }
  .login-footer { font-size: 11px; padding: 12px 16px 20px; gap: 8px; }
}
@media (max-width: 380px) {
  .login-visual { padding: 20px; grid-template-columns: minmax(0, 1fr) 98px; }
  .login-visual h1 { font-size: 21px; }
  .login-illustration { width: 116px; height: 112px; }
  .login-panel { padding: 26px 20px; }
}
@media (prefers-reduced-motion: reduce) {
  .login-panel :deep(.el-input__wrapper) { transition: none; }
}
</style>
