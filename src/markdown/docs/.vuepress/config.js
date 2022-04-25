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
  ],
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
    locales: {
      '/': {
        selectLanguageText: 'EN',
        navbar: [
          {
            text: 'Documentation',
            link: '/',
            activeMatch: '/',
          },
        ],
        sidebar: [
          createGroup('Getting Started', '/'),
          createGroup('Configuration', 'configuration', {
            children: [
              createGroup('Data Providers', 'configuration/providers'),
              createGroup('APIs', 'configuration/services', {
                children: [
                  createGroup('OGC API Building Blocks', 'configuration/services/building-blocks', {collapsible: true})
                ]
              }),
            ]
          }),
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
        ],
        sidebar: [
          createGroup('Erste Schritte', 'de'),
          createGroup('Konfiguration', 'de/configuration', {
            children: [
              createGroup('Daten-Provider', 'de/configuration/providers'),
              createGroup('APIs', 'de/configuration/services', {
                children: [
                  createGroup('OGC API Bausteine', 'de/configuration/services/building-blocks', {collapsible: true})
                ]
              }),
            ]
          }),
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

