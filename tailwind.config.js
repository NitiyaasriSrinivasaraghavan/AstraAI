/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          500: '#15803d',
          600: '#166534',
          700: '#14532d',
          800: '#0f3d22',
          900: '#0b2e1a',
        },
        primary: {
          DEFAULT: '#1B5E20',
          dark: '#0D3B12',
          light: '#2E7D32',
          container: '#C8E6C9'
        },
        accent: {
          green: '#2E7D32',
          mint: '#E8F5E9',
          surface: '#F8FAF8',
          card: '#FFFFFF',
          border: '#E0E7E1'
        }
      }
    },
  },
  plugins: [],
}
