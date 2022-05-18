const { path } = require('@vuepress/utils');
const { createGroup } = require('./sidebar.helper.js');

module.exports = {
  base: '/',
  locales: {
    '/': {
      lang: 'en-US',
      title: 'ldproxy',
      description: 'ldproxy documentation'
    },
    '/de/': {
      lang: 'de-DE',
      title: 'ldproxy',
      description: 'ldproxy Dokumentation'
    },
  },
  plugins: [
    ['vuepress-plugin-theme-extensions', {      
    }],
    ['vuepress-plugin-locale-redirect', {
    }],
    ['vuepress-plugin-md-enhance', {
      //container: true,
      mermaid: true,
    }],
    /*['@vuepress/plugin-shiki', {
      //theme: 'material-lighter',
      theme: 'hc-light',
    }],*/
  ],
  markdown: {
    code: {
      lineNumbers: false,
    }
  },
  theme: '@vuepress/theme-default',
  //theme: path.resolve(__dirname, './theme'),
  themeConfig: {
    repo: 'interactive-instruments/ldproxy',
    //logo: 'https://vuejs.org/images/logo.png',
    editLink: false,
    darkMode: true,
    /*themeExtensions: {
      navbar: true,
      onThisPage: true,
    },*/
    themePlugins: {
      //prismjs: false,
    },
    locales: {
      '/': {
        selectLanguageText: 'EN',
        selectLanguageName: 'English',
        navbar: [
          {
            text: 'Documentation',
            link: '/',
            activeMatch: '/',
          },
          {
            text: 'Demo',
            link: 'https://demo.ldproxy.net',
          },
        ],
        sidebar: [
          createGroup('Getting Started', '/', {ignoreReadme: true}),
          createGroup('Application', 'application'),
          createGroup('APIs', 'configuration/services', {
            children: [
              createGroup('Building Blocks', 'configuration/services/building-blocks', {collapsible: true})
            ]
          }),
          createGroup('Data Providers', 'configuration/providers'),
          createGroup('Codelists', 'configuration/codelists'),
          {text: 'Advanced Topics'},
        ],
        themeExtensions: {
          legalNoticeUrl: 'https://www.interactive-instruments.de/en/about/impressum/',
          privacyNoticeUrl: 'https://www.interactive-instruments.de/en/about/datenschutzerklarung/',
        }
      },
      '/de/': {
        selectLanguageText: 'DE',
        selectLanguageName: 'Deutsch',
        navbar: [
          {
            text: 'Dokumentation',
            link: '/',
            activeMatch: '/',
          },
          {
            text: 'Demo',
            link: 'https://demo.ldproxy.net',
          },
        ],
        sidebar: [
          createGroup('Erste Schritte', 'de', {ignoreReadme: true}),
          createGroup('Applikation', 'de/application'),
          createGroup('APIs', 'de/configuration/services', {
            children: [
              createGroup('Bausteine', 'de/configuration/services/building-blocks', {collapsible: true})
            ]
          }),
          createGroup('Daten-Provider', 'de/configuration/providers'),
          createGroup('Codelisten', 'de/configuration/codelists'),
          {text: 'Fortgeschrittene Themen'},
        ],
        themeExtensions: {
          onThisPageLabel: 'Auf dieser Seite',
          legalNoticeLabel: 'Impressum',
          legalNoticeUrl: 'https://www.interactive-instruments.de/de/about/impressum/',
          privacyNoticeLabel: 'Datenschutzerklärung',
          privacyNoticeUrl: 'https://www.interactive-instruments.de/de/about/datenschutzerklarung/',
        }
      },
    },
  },
  host: '127.0.0.1'
}

