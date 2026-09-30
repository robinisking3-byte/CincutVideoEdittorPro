/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        studio: {
          700: '#334155',
          750: '#243042',
          800: '#1e293b',
          850: '#131b2a',
          900: '#0f172a',
          950: '#07090e'
        }
      },
      animation: {
        'spin-slow': 'spin 6s linear infinite',
        'pulse-fast': 'pulse 1.2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      }
    },
  },
  plugins: [],
}
