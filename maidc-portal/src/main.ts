import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/assets/styles/global.css'
import '@/assets/styles/dark-tech.scss'
import '@/assets/styles/main.scss'
import App from './App.vue'
import router from './router'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(ElementPlus, {
  locale: zhCn,
})

// Global error handler: suppress parentNode errors during SPA route transitions
// caused by Ant Design Vue's DOM cleanup (Teleport/overlay removal).
// Unlike onErrorCaptured, the global handler does NOT interrupt Vue's rendering pipeline.
app.config.errorHandler = (err) => {
  if (err instanceof TypeError && err.message?.includes('parentNode')) {
    return
  }
  console.error('[MAIDC]', err)
}

app.mount('#app')
