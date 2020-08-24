const neutrino = require('neutrino');

//console.log(`BABEL: ${module.id}`)

//for storybook
module.exports = (api) => { 
    api.cache(true);
    const babel = neutrino().babel();
    // Remove config that cant be global.
    const { cacheDirectory, configFile, ...rest } = babel;
    return rest;
};

