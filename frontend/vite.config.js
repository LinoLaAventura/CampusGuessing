import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 告诉 Vite：所有以 /users 开头的请求，都转发给 http://localhost:8080
      '/users': {
        target: 'http://localhost:8080/api',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})