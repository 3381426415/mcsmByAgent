import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { VitePWA } from 'vite-plugin-pwa'




// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        // 关键：包含所有图片格式
        globPatterns: ['**/*.{js,css,html,png,jpg,jpeg,svg,webp}'],
        // 把缓存限制放大，防止大图被漏掉
        maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
      },
      // 这里的资源会随 Service Worker 启动时立即预加载（Precache）
      includeAssets: ['favicon.ico', 'apple-touch-icon.png'], 
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
   server: {
    host: '0.0.0.0', // 允许外部访问
    port: 8080,
    strictPort: false,
    // 可选：允许跨域
    cors: true
  },
})
