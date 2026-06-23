import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: '/api/vue/',
  build: {
    outDir: '../dist',
    assetsDir: 'assets',
    charset: 'utf-8'
  }
})