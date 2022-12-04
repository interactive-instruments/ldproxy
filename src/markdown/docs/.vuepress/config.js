const { path } = require('@vuepress/utils');
const { createGroup } = require('./sidebar.helper.js');

const sidebar = lang => { 
  const root = lang == '/' ? '' : 'de';
  const prefix = lang == 'en' ? '' : 'de/';

  return [
    createGroup({en: 'Getting Started', de: 'Erste Schritte'}[lang], root, {ignoreReadme: true}),
    createGroup({en: 'Application', de: 'Applikation'}[lang], prefix + 'application'),
    createGroup({en: 'APIs', de: 'APIs'}[lang], prefix + 'services', {
      children: [
        createGroup({en: 'Building Blocks', de: 'Bausteine'}[lang], prefix + 'services/building-blocks', {collapsible: true, headerReadme: true})
      ]
    }),
    createGroup({en: 'Data Providers', de: 'Daten-Provider'}[lang], prefix + 'providers', {
      children: [
        createGroup({en: 'Features', de: 'Features'}[lang], prefix + 'providers/feature', {headerReadme: true, 
          children: [
            createGroup({en: 'Extensions', de: 'Erweiterungen'}[lang], prefix + 'providers/feature/extensions', {collapsible: true, headerReadme: true})
          ]
        }),
        createGroup({en: 'Tiles', de: 'Tiles'}[lang], prefix + 'providers/tile', {headerReadme: true}),
      ]
    }),
    createGroup({en: 'Auxiliaries', de: 'Zubehör'}[lang], prefix + 'auxiliaries', {ignoreReadme: true}),
    {text: {en: 'Advanced', de: 'Fortgeschritten'}[lang]},
  ]
};

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
        sidebar: sidebar('en'),
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
        sidebar: sidebar('de'),
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

