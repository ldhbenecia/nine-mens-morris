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
};
