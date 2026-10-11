import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import svgr from 'vite-plugin-svgr';

const BASE = '/nine-mens-morris/';

// preview 는 command 가 serve 로 들어옴. 빌드와 같은 base 를 주지 않으면 빌드 결과의 자산 경로가 전부 404
export default defineConfig(({ command, isPreview }) => ({
  base: command === 'build' || isPreview ? BASE : '/',
  plugins: [react(), tailwindcss(), svgr()],
  cacheDir: './.vite',
  resolve: {
    alias: [{ find: '~', replacement: '/src' }],
  },
}));
