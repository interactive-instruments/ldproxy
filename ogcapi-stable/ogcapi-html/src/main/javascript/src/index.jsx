/* eslint-disable no-undef, no-underscore-dangle */
import React from "react";
import ReactDOM from "react-dom";
import FilterEditor from "./components/FilterEditor/FetchingSpatialTemporal";
import MapLibre from "./components/MapLibre";

// TODO: enable other apps for dev server
const Component = process.env.APP === "maplibre" ? MapLibre : FilterEditor;

ReactDOM.render(
  <React.StrictMode>
    <Component />
  </React.StrictMode>,
  document.getElementById("root")
);
