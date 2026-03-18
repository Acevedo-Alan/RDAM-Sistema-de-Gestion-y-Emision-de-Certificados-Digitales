/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#005EA2',
          dark: '#0F4A7C',
          light: '#2378C3',
        },
        success: '#00A91C',
        warning: '#FFBE2E',
        error: '#D54309',
        gray: {
          90: '#1B1B1B',
          70: '#454545',
          50: '#71767A',
          10: '#DCDEE0',
        },
      },
    },
  },
  plugins: [],
}
