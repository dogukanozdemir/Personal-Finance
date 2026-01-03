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
        background: '#0a0a0a',
        card: '#151515',
        'card-hover': '#1a1a1a',
        primary: '#3b82f6',
        accent: '#8b5cf6',
        success: '#10b981',
        danger: '#ef4444',
        text: '#f5f5f5',
        'text-muted': '#a3a3a3',
      },
    },
  },
  plugins: [],
}

