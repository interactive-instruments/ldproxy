/* eslint-disable prefer-template */
import React from "react";
import ReactDOM from "react-dom";

import { useMaplibreUIEffect } from "react-maplibre-ui";

const CanvasPlugin = ({ children }) => {
  useMaplibreUIEffect(({ map, maplibre }) => {
    const canvas = map.getCanvasContainer();
    const wrapper = document.createElement("div");
    wrapper.className = "canvas-container";
    canvas.appendChild(wrapper);

    const childrenWithMap = React.cloneElement(children, { map, maplibre });

    ReactDOM.render(<>{childrenWithMap}</>, wrapper);
  }, []);

  return null;
};

CanvasPlugin.displayName = "CanvasPlugin";

CanvasPlugin.propTypes = {};

CanvasPlugin.defaultProps = {};

export default CanvasPlugin;
