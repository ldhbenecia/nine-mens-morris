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
        phase:
          'linear-gradient(90deg, rgba(255,255,255,1) 0%, rgba(31,41,55,1) 20%, rgba(31,41,55,1) 80%, rgba(255,255,255,1) 100%)',
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
        'stone-set': {
          '0%': { transform: 'scale(105%)' },
          '100%': { transform: 'scale(100%)' },
        },
        'stone-set-effect': {
          '0%': { transform: 'scale(100%)', opacity: 0.2 },
          '100%': { transform: 'scale(400%)', opacity: 0 },
        },
      },
      animation: {
        'modal-background': 'fade-in 0.25s ease forwards',
        modal: 'move-up 0.25s ease forwards',
        'stone-set': 'stone-set 0.25s ease-out forwards',
        'stone-set-effect': 'stone-set-effect 0.75s ease-out forwards',
      },
      screens: {
        xs: '430px',
      },
    },
  },
  plugins: [],
};
