/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                apple: {
                    gray: '#f5f5f7',
                    dark: '#1d1d1f',
                    blue: '#0071e3',
                    orange: '#ff9500', // Requested orange accent
                }
            },
            fontFamily: {
                sans: [
                    '-apple-system',
                    'BlinkMacSystemFont',
                    '"Segoe UI"',
                    'Roboto',
                    '"Helvetica Neue"',
                    'Arial',
                    'sans-serif',
                ],
            },
            backdropBlur: {
                xs: '2px',
            }
        },
    },
    plugins: [],
}
