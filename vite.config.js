import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Without this file Vite never loads @vitejs/plugin-react and JSX is compiled
// with the classic runtime, which needs `import React` in every file.
export default defineConfig({
  plugins: [react()],
})
