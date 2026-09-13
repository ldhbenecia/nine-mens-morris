import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';

// GitHub Pages 가 ldhbenecia.github.io/nine-mens-morris/ 로 서비스하므로
// 에셋 경로가 그 하위에서 시작해야 한다. 루트(/)로 두면 전부 404 가 된다
// 로컬 개발 서버는 루트를 쓰므로 빌드 때만 적용
const BASE = '/nine-mens-morris/';

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  base: command === 'build' ? BASE : '/',
  plugins: [react(), svgr()],
  cacheDir: './.vite',
  resolve: {
    alias: [{ find: '~', replacement: '/src' }],
  },
}));
