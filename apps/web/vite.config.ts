import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const configuredBase = loadEnv(mode, '.', '').VITE_BASE_PATH || '/'
  if (!configuredBase.startsWith('/') || configuredBase.startsWith('//') || /[?#]/.test(configuredBase)) {
    throw new Error('VITE_BASE_PATH must be an absolute URL path, such as /todorok/')
  }
  const base = `${configuredBase.replace(/\/+$/, '')}/`
  return {
    base,
    define: { 'import.meta.env.VITE_APP_BUILD_ID': JSON.stringify(crypto.randomUUID()) },
    plugins: [
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        manifest: false,
        workbox: {
          navigateFallback: `${base}index.html`,
          importScripts: [`${base}push-worker.js`],
        },
      }),
    ],
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      // Each React/jsdom worker is CPU-heavy; avoid starving async UI commits.
      maxWorkers: 1,
      testTimeout: 20000,
    },
  }
})
