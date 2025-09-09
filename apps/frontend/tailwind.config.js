/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
      colors: {
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        'theme-bg': 'var(--theme-bg)',
        'theme-text': 'var(--theme-text)',
        'theme-secondary': 'var(--theme-secondary)',
        'theme-dark-gray': 'var(--theme-dark-gray)',
        'theme-light-gray': 'var(--theme-light-gray)',
        'theme-dark-gray-40': 'var(--theme-dark-gray-40)',
        'theme-destructive': 'var(--theme-destructive)',
      },
      fontFamily: {
        inter: ['Inter', 'sans-serif'],
        'roboto-mono': ['Roboto Mono', 'monospace'],
      },
      fontSize: {
        // Display sizes
        'display-lg': [
          '48px',
          { lineHeight: '60px', letterSpacing: '-2px', fontWeight: '900' },
        ],
        'display-md': [
          '36px',
          { lineHeight: '45px', letterSpacing: '-2%', fontWeight: '900' },
        ],
        'display-sm': [
          '30px',
          { lineHeight: '39px', letterSpacing: '-1px', fontWeight: '900' },
        ],

        // Heading sizes
        'heading-xl': [
          '28px',
          { lineHeight: '35px', letterSpacing: '0%', fontWeight: '700' },
        ],
        'heading-lg': [
          '24px',
          { lineHeight: '31px', letterSpacing: '0%', fontWeight: '700' },
        ],
        'heading-md': [
          '20px',
          { lineHeight: '27px', letterSpacing: '0%', fontWeight: '700' },
        ],
        'heading-base': [
          '18px',
          { lineHeight: '23px', letterSpacing: '0%', fontWeight: '700' },
        ],
        'heading-sm': [
          '16px',
          { lineHeight: '23px', letterSpacing: '0%', fontWeight: '700' },
        ],
        'heading-xs': [
          '14px',
          { lineHeight: '21px', letterSpacing: '0%', fontWeight: '700' },
        ],

        // Body sizes
        'body-xl': [
          '20px',
          { lineHeight: '30px', letterSpacing: '0%', fontWeight: '400' },
        ],
        'body-lg': [
          '18px',
          { lineHeight: '27px', letterSpacing: '0%', fontWeight: '400' },
        ],
        'body-md': [
          '16px',
          { lineHeight: '24px', letterSpacing: '0%', fontWeight: '400' },
        ],
        'body-sm': [
          '14px',
          { lineHeight: '21px', letterSpacing: '0%', fontWeight: '400' },
        ],
        'body-xs': [
          '12px',
          { lineHeight: '18px', letterSpacing: '0%', fontWeight: '400' },
        ],
        'body-xs-auto': [
          '12px',
          { lineHeight: 'normal', letterSpacing: '0%', fontWeight: '400' },
        ],

        // Caption sizes
        'caption-md': [
          '12px',
          { lineHeight: '16px', letterSpacing: '-2%', fontWeight: '500' },
        ],
        'caption-sm': [
          '10px',
          { lineHeight: '14px', letterSpacing: '0px', fontWeight: '500' },
        ],

        // Overline sizes
        'overline-md': [
          '16px',
          { lineHeight: '24px', letterSpacing: '0px', fontWeight: '500' },
        ],
        'overline-sm': [
          '14px',
          { lineHeight: '21px', letterSpacing: '0px', fontWeight: '500' },
        ],
        'overline-xs': [
          '12px',
          { lineHeight: '23px', letterSpacing: '0px', fontWeight: '500' },
        ],

        // Code sizes
        'code-base': [
          '12px',
          { lineHeight: 'normal', letterSpacing: '0%', fontWeight: '400' },
        ],
        'code-xs': [
          '10px',
          { lineHeight: '23px', letterSpacing: '0px', fontWeight: '400' },
        ],
      },
      boxShadow: {
        default: '0px 2px 10px 0px #14141440',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
};
