import { defineUserConfig, defaultTheme } from 'vuepress';
import { docsearchPlugin } from '@vuepress/plugin-docsearch';
import { mdEnhancePlugin } from "vuepress-plugin-md-enhance";
import { themeDocs, createGroups } from 'vuepress-plugin-theme-extensions';

const sidebar = lang => { 
  const group = createGroups(__dirname);
  const prefix = lang == 'en' ? '' : 'de/';

  return [
    group({en: 'Getting Started', de: 'Erste Schritte'}[lang], prefix, {ignoreReadme: true}),
    group({en: 'Application', de: 'Applikation'}[lang], prefix + 'application'),
    group({en: 'APIs', de: 'APIs'}[lang], prefix + 'services', {ignoreReadme: true,
      children: [
        group({en: 'OGC API', de: 'OGC API'}[lang], prefix + 'services', {headerReadme: true, 
          children: [
            group({en: 'Building Blocks', de: 'Bausteine'}[lang], prefix + 'services/building-blocks', {collapsible: true, headerReadme: true})
          ]
        }),
      ]
    }),
    group({en: 'Data Providers', de: 'Daten-Provider'}[lang], prefix + 'providers', {
      children: [
        group({en: 'Features', de: 'Features'}[lang], prefix + 'providers/feature', {headerReadme: true, 
          children: [
            group({en: 'Extensions', de: 'Erweiterungen'}[lang], prefix + 'providers/feature/extensions', {collapsible: true, headerReadme: true})
          ]
        }),
        group({en: 'Tiles', de: 'Tiles'}[lang], prefix + 'providers/tile', {headerReadme: true}),
      ]
    }),
    group({en: 'Auxiliaries', de: 'Zubehör'}[lang], prefix + 'auxiliaries', {ignoreReadme: true}),
    {text: {en: 'Advanced', de: 'Fortgeschritten'}[lang]},
  ]
};

export default defineUserConfig({
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
    docsearchPlugin({
      appId: 'TNOB61BGZX',
      apiKey: '75d89f7da0b2ccdd1078b38607739c2b',
      indexName: 'ldproxy',
    }),
    mdEnhancePlugin({
      //container: true,
      mermaid: true,
      linkCheck: 'always',
    }),
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
  theme: themeDocs({
    navbar: true,
    tableRowHeight: true,
    onThisPage: true,
    repo: 'interactive-instruments/ldproxy',
    //logo: 'https://vuejs.org/images/logo.png',
    editLink: false,
    colorModeSwitch: false,
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
          {
            text: `v3.x`,
            children: [
              {
                text: 'v3.x',
                link: '',
                activeMatch: '/',
              },
            ],
            group: "start",
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
          {
            text: `v3.x`,
            children: [
              {
                text: 'v3.x',
                link: '',
                activeMatch: '/',
              },
            ],
            group: "start",
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
  }),
  host: '127.0.0.1'
});

