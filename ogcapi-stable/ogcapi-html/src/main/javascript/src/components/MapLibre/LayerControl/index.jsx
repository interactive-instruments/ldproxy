import React, { useState } from "react";
import PropTypes from "prop-types";
import { Button, ButtonGroup } from "reactstrap";
import { useMaplibreUIEffect } from "react-maplibre-ui";

const LayerControl = ({ layerGroups }) => {
  const [selected, setSelected] = useState(Object.keys(layerGroups));

  const onSelect = (name) => {
    const index = selected.indexOf(name);
    if (index < 0) {
      selected.push(name);
    } else {
      selected.splice(index, 1);
    }
    setSelected([...selected]);
  };

  useMaplibreUIEffect(
    ({ map }) => {
      Object.keys(layerGroups).forEach((name) => {
        layerGroups[name].forEach((layerId) => {
          if (map.getLayer(layerId)) {
            const visible =
              map.getLayoutProperty(layerId, "visibility") !== "none";

            if (visible && !selected.includes(name)) {
              map.setLayoutProperty(layerId, "visibility", "none");
            } else if (!visible && selected.includes(name)) {
              map.setLayoutProperty(layerId, "visibility", "visible");
            }
          }
        });
      });
    },
    [selected]
  );

  return (
    <div
      id="layer-control"
      style={{ position: "absolute", zIndex: 1, top: "10px", right: "50px" }}
    >
      <ButtonGroup vertical>
        {Object.keys(layerGroups).map((name) => (
          <Button
            color="secondary"
            onClick={(e) => {
              e.target.blur();
              onSelect(name);
            }}
            active={selected.includes(name)}
          >
            {name}
          </Button>
        ))}
      </ButtonGroup>
    </div>
  );
};
LayerControl.displayName = "LayerControl";

LayerControl.propTypes = {
  layerGroups: PropTypes.objectOf(PropTypes.array),
};

LayerControl.defaultProps = {
  layerGroups: {},
};

export default LayerControl;
