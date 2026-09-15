import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      host: '127.0.0.1',
      port: 5173,
      strictPort: true,
      proxy: { '/api': env.BACKEND_LOCAL || 'http://127.0.0.1:8080' },
    },
    test: { environment: 'jsdom', setupFiles: './src/test/setup.ts' },
  }
})
