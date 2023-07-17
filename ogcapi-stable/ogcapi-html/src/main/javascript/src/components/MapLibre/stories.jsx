import React from "react";

import MapLibre from ".";

export default {
  title: "@ogcapi/html/MapLibre",
  component: MapLibre,
};

const Template = (args) => <MapLibre {...args} />;

export const Plain = Template.bind({});

Plain.args = {
  styleUrl: "https://demo.ldproxy.net/daraa/styles/topographic-with-basemap?f=mbs",
  layerGroupControl: {
    TransportationGroundCrv: [
      "transportationgroundcrv.0a",
      "transportationgroundcrv.0b",
      "transportationgroundcrv.1",
      "transportationgroundcrv.2",
      "transportationgroundcrv.3",
      "transportationgroundcrv.4",
      "transportationgroundcrv.5",
      "transportationgroundcrv.6",
      "transportationgroundcrv.7",
      "transportationgroundcrv.8",
      "transportationgroundcrv.9",
    ],
  },
  /*styleUrl:
        'http://localhost:7080/rest/services/feuerwehr/v1/styles/default?f=mbs', */
};

export const Items = Template.bind({});

Items.args = {
  dataUrl:
    "http://localhost:7080/rest/services/feuerwehr/v1/collections/governmentalservice/items?f=json",
};

export const Extent = Template.bind({});

Extent.args = {
  bounds: [
    [6.3, 49.0],
    [8.4, 50.6],
  ],
  drawBounds: true,
  interactive: false,
  defaultStyle: {
    color: "red",
  },
};

export const Tiles = Template.bind({});

Tiles.args = {
  dataUrl:
    "http://localhost:7080/rest/services/feuerwehr/v1/tiles/WebMercatorQuad/{z}/{y}/{x}?f=mvt",
  dataType: "vector",
  dataLayers: { governmentalservice: ["points"] },
  // styleUrl:
  //    'http://localhost:7080/rest/services/feuerwehr/v1/styles/default?f=mbs',
  popup: "CLICK_PROPERTIES",
};
