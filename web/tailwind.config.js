/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        default: 'Pretendard',
        title: 'Kumbh Sans',
        phase: 'Big Shoulders Text',
      },
      backgroundImage: {
        'white-stone': 'radial-gradient(circle, #ffffff 25%, #e5e7eb 100%)',
        'black-stone': 'radial-gradient(circle, #4b5563 25%, #1f2937 100%)',
      },
      boxShadow: {
        stone: '0 4px 4px rgba(0,0,0,0.25)',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: 0 },
          '100%': { opacity: 1 },
        },
        'move-up': {
          '0%': { transform: 'translateY(5%)' },
          '100%': { transform: 'translateY(0%)' },
        },
      },
      animation: {
        'modal-background': 'fade-in 0.25s ease forwards',
        modal: 'move-up 0.25s ease forwards',
      },
    },
  },
  plugins: [],
};
