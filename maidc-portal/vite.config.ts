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
        '/api/v1/auth': { target: 'http://localhost:8081' },
        '/api/v1/users': { target: 'http://localhost:8081' },
        '/api/v1/roles': { target: 'http://localhost:8081' },
        '/api/v1/permissions': { target: 'http://localhost:8081' },
        '/api/v1/models': { target: 'http://localhost:8083' },
        '/api/v1/evaluations': { target: 'http://localhost:8083' },
        '/api/v1/approvals': { target: 'http://localhost:8083' },
        '/api/v1/deployments': { target: 'http://localhost:8083' },
        '/api/v1/inference': { target: 'http://localhost:8083' },
        '/api/v1/alert': { target: 'http://localhost:8083' },
        '/api/v1/monitoring': { target: 'http://localhost:8083' },
        '/api/v1/cdr': { target: 'http://localhost:8082' },
        '/api/v1/rdr': { target: 'http://localhost:8082' },
        '/api/v1/etl': { target: 'http://localhost:8082' },
        '/api/v1/quality': { target: 'http://localhost:8082' },
        '/api/v1/dict': { target: 'http://localhost:8082' },
        '/api/v1/tasks': { target: 'http://localhost:8084' },
        '/api/v1/label-tasks': { target: 'http://localhost:8085' },
        '/api/v1/audit': { target: 'http://localhost:8086' },
        '/api/v1/messages': { target: 'http://localhost:8087' },
        '/api/v1/notifications': { target: 'http://localhost:8087' },
      },
    },
    build: {
      target: 'es2020',
      cssTarget: 'chrome80',
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: mode === 'production',
          drop_debugger: true,
        },
      },
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-antd': ['ant-design-vue', '@ant-design/icons-vue'],
            'vendor-echarts': ['echarts'],
            'vendor-utils': ['axios', 'dayjs'],
          },
        },
      },
      chunkSizeWarningLimit: 1000,
      reportCompressedSize: true,
      sourcemap: mode !== 'production',
    },
  }
})
