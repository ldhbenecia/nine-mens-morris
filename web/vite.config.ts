import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';

const BASE = '/nine-mens-morris/';

export default defineConfig(({ command }) => ({
  base: command === 'build' ? BASE : '/',
  plugins: [react(), svgr()],
  cacheDir: './.vite',
  resolve: {
    alias: [{ find: '~', replacement: '/src' }],
  },
}));
