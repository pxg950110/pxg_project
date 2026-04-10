import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [vue()],
    resolve: {
      alias: { '@': resolve(__dirname, 'src') },
    },
    server: {
      port: 3000,
      proxy: {
        '/api': { target: env.VITE_API_BASE_URL || 'http://localhost:8080', changeOrigin: true },
        '/ws': { target: env.VITE_WS_URL || 'ws://localhost:8087', ws: true },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-antd': ['ant-design-vue', '@ant-design/icons-vue'],
            'vendor-echarts': ['echarts'],
          },
        },
      },
    },
  }
})
