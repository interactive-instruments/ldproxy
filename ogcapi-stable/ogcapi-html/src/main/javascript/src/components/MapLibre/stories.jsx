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
  layerGroupControl: [
    {
      id: "Basemap",
      type: "group",
      isBasemap: true,
      entries: [
        {
          id: "agriculturesrf",
          layers: ["agriculturesrf"],
        },
        {
          id: "vegetationsrf",
          layers: ["vegetationsrf"],
        },
      ],
    },
    {
      id: "All",
      type: "group",
      entries: [
        {
          id: "TransportationGroundCrv",
          type: "source-layer",
          subLayers: [
            {
              id: "transportationgroundcrv.0a",
              layers: ["transportationgroundcrv.0a"],
            },
            {
              id: "transportationgroundcrv.0b",
              layers: ["transportationgroundcrv.0b"],
            },
            {
              id: "transportationgroundcrv.1",
              layers: ["transportationgroundcrv.1"],
            },
            {
              id: "transportationgroundcrv.2",
              layers: ["transportationgroundcrv.2"],
            },
            {
              id: "transportationgroundcrv.3",
              layers: ["transportationgroundcrv.3"],
            },
            {
              id: "transportationgroundcrv.4",
              layers: ["transportationgroundcrv.4"],
            },
            {
              id: "transportationgroundcrv.5",
              layers: ["transportationgroundcrv.5"],
            },
            {
              id: "transportationgroundcrv.6",
              layers: ["transportationgroundcrv.6"],
            },
            {
              id: "transportationgroundcrv.7",
              layers: ["transportationgroundcrv.7"],
            },
            {
              id: "transportationgroundcrv.8",
              layers: ["transportationgroundcrv.8"],
            },
            {
              id: "transportationgroundcrv.9",
              layers: ["transportationgroundcrv.9"],
            },
          ],
        },

        {
          id: "Test",
          type: "source-layer",
          subLayers: [
            {
              id: "transportationground",
              layers: ["transportationground"],
            },
          ],
        },
      ],
    },
  ],
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
