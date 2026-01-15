/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          bg: '#16181D',
          card: 'rgba(30, 32, 41, 0.85)',
          input: 'rgba(0, 0, 0, 0.25)',
        },
        primary: {
          DEFAULT: '#8E2DE2',
          gradient: 'linear-gradient(90deg, #8E2DE2, #4A00E0)',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
