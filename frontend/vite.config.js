import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 允许通过本机 IP（局域网）访问 Vite 开发服务器
    // 等价于启动命令加 --host（监听 0.0.0.0）
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      // SockJS/STOMP 对战通道（显式开启 ws 转发）
      '/api/ws-battle': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
      // 后端 application.yml 配置了 context-path: /api
      // 本地开发通过代理转发，避免浏览器 CORS
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
