import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Detect if running locally (not in Docker)
// In Docker, these env vars are typically set; locally they won't be
const isLocal = !process.env.DOCKER && !process.env.CONTAINER;

export default defineConfig({
  plugins: [react()],
  server: {
    host: isLocal ? 'localhost' : '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: isLocal ? 'http://localhost:8080' : 'http://backend:8080',
        changeOrigin: true
      }
    }
  }
})

