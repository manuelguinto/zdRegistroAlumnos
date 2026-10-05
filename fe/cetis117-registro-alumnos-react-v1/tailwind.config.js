/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        cetis: {
          wine: '#941F32',
          wineDark: '#741827',
          gold: '#D8A23A',
          goldSoft: '#F6E7C7',
          cream: '#F8F3EA',
          creamLight: '#FFFDF8',
          ink: '#2E2527',
          muted: '#7A7072',
        },
      },
      boxShadow: {
        soft: '0 12px 36px rgba(91, 55, 45, 0.10)',
      },
    },
  },
  plugins: [],
}
