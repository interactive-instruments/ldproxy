const airbnb = require("@neutrinojs/airbnb");
const react = require("@neutrinojs/react");
const mocha = require("@neutrinojs/mocha");
const xtraplatform = require("@xtraplatform/neutrino");
const package = require("./package.json");

module.exports = {
  options: {
    root: __dirname,
    output: "../../../build/generated/src/main/resources/manager",
  },
  use: [
    airbnb(),
    react({
      html: {
        title: `${package.name} ${package.version}`,
      },
      publicPath: "",
    }),
    mocha(),
    xtraplatform({
      lib: false,
      modulePrefixes: ["ogcapi", "@ogcapi"],
    }),
  ],
};
