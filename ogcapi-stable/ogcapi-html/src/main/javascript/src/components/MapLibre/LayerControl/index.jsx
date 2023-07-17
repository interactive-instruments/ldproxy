import React, { useState } from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";
import { useMaplibreUIEffect } from "react-maplibre-ui";

const LayerControl = ({ layerGroups }) => {
  const [selected, setSelected] = useState(Object.keys(layerGroups));
  const [open, setOpen] = useState([]);

  const onSelect = (name) => {
    const index = selected.indexOf(name);
    if (index < 0) {
      selected.push(name);
    } else {
      selected.splice(index, 1);
    }
    setSelected([...selected]);
  };

  const onOpen = (name) => {
    const index = open.indexOf(name);
    if (index < 0) {
      open.push(name);
    } else {
      open.splice(index, 1);
    }
    setOpen([...open]);
  };

  useMaplibreUIEffect(
    ({ map }) => {
      Object.keys(layerGroups).forEach((name) => {
        layerGroups[name].forEach((layerId) => {
          if (map.getLayer(layerId)) {
            const visible = map.getLayoutProperty(layerId, "visibility") !== "none";

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

  const isSubLayerOpen = (name) => {
    return open.includes(name);
  };

  return (
    <div
      className="accordion"
      id="layer-control"
      style={{ position: "absolute", zIndex: 1, top: "40px", left: "30px" }}
    >
      {Object.keys(layerGroups).map((name) => (
        <div className="accordion-item" key={name}>
          <h2 className="accordion-header" id={name}>
            <button
              style={{
                backgroundColor: "white",
                borderRadius: "0.25rem",
                padding: "10px",
                paddingLeft: "2px",
              }}
              color="secondary"
              outline
              onClick={(e) => {
                e.target.blur();
                onSelect(name);
                onOpen(name);
              }}
              active={isSubLayerOpen}
              className={`accordion-button ${isSubLayerOpen(name) ? "collapsed" : ""}`}
              type="button"
              data-bs-toggle="collapse"
              data-bs-target={`#collapse-${name}`}
              aria-expanded={isSubLayerOpen(name)}
              aria-controls={`collapse-${name}`}
            >
              <span style={{ marginRight: "10px" }}>{name}</span>
            </button>
          </h2>
          {layerGroups[name].map((subLayer) => (
            <Collapse
              isOpen={isSubLayerOpen(name)}
              id={`collapse-${subLayer}`}
              key={subLayer}
              className="accordion-collapse"
              aria-labelledby={`heading-${subLayer}`}
              data-bs-parent="#layer-control"
            >
              <span style={{ marginLeft: "25px" }}>{subLayer}</span>
            </Collapse>
          ))}
        </div>
      ))}
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
