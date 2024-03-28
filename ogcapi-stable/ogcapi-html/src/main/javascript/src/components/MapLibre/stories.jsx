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
  // styleUrl: "https://demo.ldproxy.net/strassen/styles/default?f=mbs",
  // styleUrl:
  // "http://localhost:7080/daraa/styles/topographic-with-basemap-layercontrol?f=mbs",
  /* layerGroupControl: [
    {
      id: "Abschnitte und Äste",
      type: "merge-group",
      entries: [
        "Kontur_Autobahn_Abfahrt",
        "Kontur_Autobahn_Abfahrt_Ast",
        "Kontur_Kreisstr_einblenden",
        "Kontur_Kreisstr_einblenden_Ast",
        "Kontur_Kreisstr",
        "Kontur_Kreisstr_Ast",
        "Kontur_Landesstr_Staatsstr_einblenden",
        "Kontur_Landesstr_Staatsstr",
        "Kontur_Landesstr_Staatsstr_Ast",
        "Kontur_Bundesstr_einblenden",
        "Kontur_Bundesstr",
        "Kontur_Bundesstr_Ast",
        "Kontur_Autobahn_getrennt",
        "Kontur_Autobahn_getrennt_Ast",
        "Decker_Autobahn_Abfahrt_einblenden",
        "Decker_Autobahn_Abfahrt_einblenden_Ast",
        "Decker_Autobahn_Abfahrt",
        "Decker_Autobahn_Abfahrt_Ast",
        "Decker_Kreisstr",
        "Decker_Kreisstr_Ast",
        "Decker_Landesstr_Staatsstr_ohne_Tunnel_Bruecke",
        "Decker_Landesstr_Staatsstr_ohne_Tunnel_Bruecke_Ast",
        "Decker_Landesstr_Staatsstr",
        "Decker_Landesstr_Staatsstr_Ast",
        "Decker_Bundesstr_ohne_Tunnel_Bruecke",
        "Decker_Bundesstr_ohne_Tunnel_Bruecke_Ast",
        "Decker_Bundesstr",
        "Decker_Bundesstr_Ast",
        "Decker_Autobahn_getrennt_ohne_Tunnel_Bruecke",
        "Decker_Autobahn_getrennt_ohne_Tunnel_Bruecke_Ast",
        "Decker_Autobahn_getrennt",
        "Decker_Autobahn_getrennt_Ast",
        "Decker_Autobahn_getrennt_Fahrbahnachse",
        "Decker_Autobahn_getrennt_Fahrbahnachse_Ast",
        "Mittellinie_Autobahn",
        "Mittellinie_Autobahn_Ast",
        "Mittellinie_Bundesstrasse",
        "Mittellinie_Bundesstrasse_Ast",
      ],
    },
    {
      id: "Hintergrundkarte",
      type: "merge-group",
      entries: ["Hintergrundkarte"],
    },
    {
      id: "Unfälle 2019",
      type: "merge-group",
      entries: ["Unfälle 2019", "Unfälle2"],
    },
  ], */
  layerGroupControl: [
    {
      id: "outer",
      label: "OUTER",
      type: "group",
      onlyLegend: true,
      entries: [
        {
          id: "Railway",
          type: "merge-group",
          entries: [
            {
              id: "transportationgroundcrv.0a",
            },
            {
              id: "transportationgroundcrv.0b",
            },
          ],
        },
        {
          id: "Hydro",
          type: "group",
          entries: [
            "hydrographycrv",
            {
              id: "hydrographysrf",
            },
          ],
        },
      ],
    },
    {
      id: "Basemap",
      type: "radio-group",
      entries: [
        "utilityinfrastructurepnt",
        {
          id: "agriculturesrf",
        },
      ],
    },
    {
      id: "Transportation",
      type: "merge-group",
      sourceLayer: "TransportationGroundCrv",
    },
    {
      id: "Settlement",
      type: "merge-group",
      sourceLayer: "SettlementSrf",
    },
    "militarysrf",
    {
      id: "All2",
      type: "group",
      entries: [
        {
          id: "TransportationGroundCrv",
          type: "group",
          entries: [
            {
              id: "transportationgroundcrv.1",
              zoom: 5,
            },
            {
              id: "transportationgroundcrv.2",
            },
            {
              id: "transportationgroundcrv.3",
            },
            {
              id: "transportationgroundcrv.4",
            },
            {
              id: "transportationgroundcrv.5",
            },
            {
              id: "transportationgroundcrv.6",
            },
            {
              label: "Foo",
              id: "transportationgroundcrv.7",
            },
            {
              label: "Bar",
              id: "transportationgroundcrv.8",
            },
            {
              id: "transportationgroundcrv.9",
            },
          ],
        },
        {
          id: "Test",
          type: "group",
          entries: [
            {
              id: "transportationground",
            },
          ],
        },
      ],
    },
  ],
};

/*
export const Items = Template.bind({});

Items.args = {
    dataUrl:
    "http://localhost:7080/feuerwehr/v1/collections/governmentalservice/items?f=json",
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
    "http://localhost:7080/feuerwehr/v1/tiles/WebMercatorQuad/{z}/{y}/{x}?f=mvt",
  dataType: "vector",
  dataLayers: { governmentalservice: ["points"] },
    // styleUrl:
    //    'http://localhost:7080/feuerwehr/v1/styles/default?f=mbs',
  popup: "CLICK_PROPERTIES",
}; */
