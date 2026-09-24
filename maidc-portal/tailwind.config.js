/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        clinical: {
          primary: '#0EA5E9',
          'primary-dark': '#0284C7',
          navy: '#0F172A',
          surface: '#F8FAFC',
          card: '#FFFFFF',
          border: '#E2E8F0',
          success: '#10B981',
          warning: '#F59E0B',
          danger: '#EF4444',
          purple: '#8B5CF6'
        }
      },
      boxShadow: {
        'clinical-sm': '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
        'clinical': '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.05)',
        'clinical-lg': '0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.05)',
      }
    },
  },
  plugins: [],
}
