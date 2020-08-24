const airbnb = require('@neutrinojs/airbnb');
const react = require('@neutrinojs/react');
const mocha = require('@neutrinojs/mocha');
const xtraplatform = require('@xtraplatform/neutrino');
const package = require('./package.json')

module.exports = {
  options: {
    root: __dirname,
  },
  use: [
    airbnb(),
    react({
      html: {
        title: `${package.name} ${package.version}`
      }
    }),
    mocha(),
    xtraplatform({
      lib: false,
      modulePrefixes: ['ogcapi']
    }),
  ],
};
