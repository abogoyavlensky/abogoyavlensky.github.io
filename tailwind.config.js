module.exports = {
  purge: {
    mode: 'all',
    content:[
        {
          raw: '<html><body><div><a><ul><ol><li><article><script><title><blockquote><br><b><font><i><pre><code>'
               + '<input><textarea><link><meta><head><header><footer><span><p><h1><h2><h3><h4><h5><h6><del>'
               + '<strong><sub><sup><from><button><select><label><option><iframe><img><svg><audio><source>'
               + '<track><video><link><nav><table><caption><th><tr><td><thead><tbody><col><style><section>'
               + '<main><article><base><script>',
          extension: 'html'
        },
        './src/**/*.clj',
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
  future: {
    removeDeprecatedGapUtilities: true
  }
}
