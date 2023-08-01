import React from "react";
import ReactDOM from "react-dom";
import PropTypes from "prop-types";
import { useMaplibreUIEffect } from "react-maplibre-ui";
import Control from "./Control";

const LayerControl = ({ layerGroups }) => {
  useMaplibreUIEffect(({ map }) => {
    const container = document.createElement("div");

    map.addControl(
      {
        onAdd: () => {
          ReactDOM.render(
            <React.StrictMode>
              <Control
                layerGroups={layerGroups}
                mapHeight={map.getContainer().offsetHeight}
                map={map}
              />
            </React.StrictMode>,
            container
          );
          return container;
        },
        onRemove: () => container.parentNode.removeChild(container),
      },
      "top-left"
    );
  }, []);

  return null;
};

LayerControl.displayName = "LayerControl";

LayerControl.propTypes = {
  layerGroups: PropTypes.arrayOf(PropTypes.object),
};

LayerControl.defaultProps = {
  layerGroups: [],
};

export default LayerControl;
