/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './frontend/public/**/*.html',
    './frontend/public/**/*.js',
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
