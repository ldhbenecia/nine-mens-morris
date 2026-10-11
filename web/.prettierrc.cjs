module.exports = {
  printWidth: 80,
  tabWidth: 2,
  singleQuote: true,
  trailingComma: 'es5',
  useTabs: false,
  arrowParens: 'always',
  bracketSpacing: true,
  bracketSameLine: false,
  plugins: [require.resolve('prettier-plugin-tailwindcss')],
  // Tailwind 4 는 설정이 CSS 에 있어 클래스 정렬 기준을 여기서 읽음
  tailwindStylesheet: './src/styles/index.css',
};
