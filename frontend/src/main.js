import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'

// 前端入口：挂载 Vue、Element Plus 与路由。
createApp(App).use(router).use(ElementPlus).mount('#app')
