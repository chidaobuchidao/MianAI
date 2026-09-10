import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const backendTarget = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: ''
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
        // SSE streaming support: disable buffering, extend timeout
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            // Same-origin browser requests remain same-origin after the dev proxy
            // rewrites Host, including when Vite runs on an alternate local port.
            const origin = req.headers.origin
            if (origin === `http://${req.headers.host}` || origin === `https://${req.headers.host}`) {
              proxyReq.setHeader('Origin', backendTarget)
            }
            // Prevent proxy from buffering SSE streams
            if (req.headers.accept === 'text/event-stream') {
              proxyReq.setHeader('Connection', 'keep-alive')
            }
          })
          proxy.on('proxyRes', (proxyRes) => {
            // Disable nagle algorithm for real-time streaming
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      }
    }
  }
})
