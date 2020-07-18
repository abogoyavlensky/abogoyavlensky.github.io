module.exports = {
  purge: [],
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
