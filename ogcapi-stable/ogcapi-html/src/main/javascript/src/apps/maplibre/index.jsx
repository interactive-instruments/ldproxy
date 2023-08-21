/* eslint-disable no-undef, no-underscore-dangle */
import React from "react";
import ReactDOM from "react-dom";
import "core-js";
import MapLibre from "../../components/MapLibre";

if (globalThis._map && globalThis._map.container) {
  ReactDOM.render(
    <React.StrictMode>
      <MapLibre {...globalThis._map} />
    </React.StrictMode>,
    document.getElementById(global._map.container)
  );
}
