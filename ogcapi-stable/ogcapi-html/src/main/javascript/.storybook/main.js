module.exports = {
    stories: [
        '../**/stories.@(js|jsx|ts|tsx)',
    ],
    addons: ['@storybook/addon-links', '@storybook/addon-essentials'],
  webpackFinal: (config) => {
    config.module.rules[0].exclude = /node_modules(?!(\/|\\)@xtraplatform)/
    return config;
  },
};
