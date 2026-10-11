import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import svgr from 'vite-plugin-svgr';

const BASE = '/nine-mens-morris/';

export default defineConfig(({ command }) => ({
  base: command === 'build' ? BASE : '/',
  plugins: [react(), tailwindcss(), svgr()],
  cacheDir: './.vite',
  resolve: {
    alias: [{ find: '~', replacement: '/src' }],
  },
}));
