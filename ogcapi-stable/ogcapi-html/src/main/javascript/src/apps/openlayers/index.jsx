/* eslint-disable no-undef, no-underscore-dangle */
import React from "react";
import ReactDOM from "react-dom";
import OpenLayers from "../../components/OpenLayers";

if (globalThis._map && globalThis._map.container) {
  ReactDOM.render(
    <React.StrictMode>
      <OpenLayers {...globalThis._map} />
    </React.StrictMode>,
    document.getElementById(global._map.container)
  );
}
