module.exports = {
  purge: {
    mode: 'all',
    content:[
        './dist/**/*.html',
    ],
  },
  theme: {
    extend: {},
    typography: (theme) => ({
      default: {
        css: {
          color: theme('colors.gray.800'),
        }
      }
    })
  },
  variants: {},
  plugins: [
    require('@tailwindcss/typography')
  ],
}
