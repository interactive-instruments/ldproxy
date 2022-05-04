const { path } = require('@vuepress/utils')
const { vuepressOnThisPage, Layout } = require('vuepress-onthispage');

module.exports = {
  name: 'vuepress-theme-ldproxy',
  extends: '@vuepress/theme-default',
  layouts: {
    //Layout: path.resolve(__dirname, 'layouts/Layout.vue'),
    Layout: Layout,
  },
  plugins: [
    [vuepressOnThisPage, {
    }],
  ]
}
