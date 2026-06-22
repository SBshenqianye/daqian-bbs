/** @type {import('tailwindcss').Config} */
module.exports = {
  purge: [
    "./public/index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Material Design 3 color tokens
        'primary': '#0052d9',
        'primary-container': '#0052d9',
        'on-primary': '#ffffff',
        'on-primary-container': '#cbd6ff',
        'on-primary-fixed': '#00174b',
        'on-primary-fixed-variant': '#003ea8',
        'primary-fixed': '#dbe1ff',
        'primary-fixed-dim': '#b4c5ff',
        'inverse-primary': '#b4c5ff',

        'secondary': '#555f6a',
        'secondary-container': '#d9e3f1',
        'on-secondary': '#ffffff',
        'on-secondary-container': '#5b6571',
        'on-secondary-fixed': '#131c26',
        'on-secondary-fixed-variant': '#3e4852',
        'secondary-fixed': '#d9e3f1',
        'secondary-fixed-dim': '#bdc7d4',

        'tertiary': '#822600',
        'tertiary-container': '#aa3500',
        'on-tertiary': '#ffffff',
        'on-tertiary-container': '#ffccbc',
        'on-tertiary-fixed': '#390c00',
        'on-tertiary-fixed-variant': '#832700',
        'tertiary-fixed': '#ffdbd0',
        'tertiary-fixed-dim': '#ffb59c',

        'error': '#ba1a1a',
        'error-container': '#ffdad6',
        'on-error': '#ffffff',
        'on-error-container': '#93000a',

        'background': '#f8f9fb',
        'on-background': '#181c23',

        'surface': '#f8f9fb',
        'surface-dim': '#d7dae5',
        'surface-bright': '#f9f9ff',
        'surface-variant': '#dfe2ed',
        'on-surface': '#181c23',
        'on-surface-variant': '#434654',
        'inverse-surface': '#2c3039',
        'inverse-on-surface': '#eef0fc',
        'surface-tint': '#0353da',

        'surface-container': '#ebedf9',
        'surface-container-low': '#f1f3ff',
        'surface-container-lowest': '#ffffff',
        'surface-container-high': '#e5e8f3',
        'surface-container-highest': '#dfe2ed',

        'outline': '#737686',
        'outline-variant': '#c3c6d7',

        'container': '#ffffff',
        'border': '#d9dadc',

        'rank-gold': '#ffd700',
        'rank-silver': '#c0c0c0',
        'rank-bronze': '#cd7f32',

        'brand-blue': '#0052d9',
      },
      borderRadius: {
        'DEFAULT': '8px',
        'lg': '8px',
        'xl': '12px',
        'full': '9999px',
      },
      spacing: {
        'page-margin-desktop': '32px',
        'page-margin-mobile': '16px',
        'card-padding': '20px',
        'gutter': '24px',
        'component-gap': '12px',
      },
      fontFamily: {
        'headline-sm': ['Manrope', 'sans-serif'],
        'headline-lg': ['Manrope', 'sans-serif'],
        'headline-lg-mobile': ['Manrope', 'sans-serif'],
        'label-md': ['Manrope', 'sans-serif'],
        'headline-md': ['Manrope', 'sans-serif'],
        'body-lg': ['Manrope', 'sans-serif'],
        'body-md': ['Manrope', 'sans-serif'],
        'sans': ['Manrope', 'sans-serif'],
      },
      fontSize: {
        'headline-sm': ['18px', { lineHeight: '24px', fontWeight: '600' }],
        'headline-lg': ['24px', { lineHeight: '32px', fontWeight: '600' }],
        'headline-lg-mobile': ['20px', { lineHeight: '28px', fontWeight: '600' }],
        'label-md': ['12px', { lineHeight: '16px', fontWeight: '500' }],
        'headline-md': ['20px', { lineHeight: '28px', fontWeight: '600' }],
        'body-lg': ['16px', { lineHeight: '1.6', fontWeight: '400' }],
        'body-md': ['14px', { lineHeight: '1.6', fontWeight: '400' }],
      },
    },
  },
  plugins: [],
}
