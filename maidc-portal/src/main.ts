import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/assets/styles/global.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)

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
