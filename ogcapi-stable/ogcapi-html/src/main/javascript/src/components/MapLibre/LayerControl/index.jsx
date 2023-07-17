import React, { useState } from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";
import { useMaplibreUIEffect } from "react-maplibre-ui";

const LayerControl = ({ layerGroups }) => {
  const [selected, setSelected] = useState(
    layerGroups[0].entries.map((entry) => {
      return entry.id;
    })
  );
  const [open, setOpen] = useState([]);

  const onSelect = (entry) => {
    const index = selected.indexOf(entry.id);
    if (index < 0) {
      selected.push(entry.id);
    } else {
      selected.splice(index, 1);
    }
    setSelected([...selected]);
  };

  const onOpen = (entry) => {
    const index = open.indexOf(entry.id);
    if (index < 0) {
      open.push(entry.id);
    } else {
      open.splice(index, 1);
    }
    setOpen([...open]);
  };

  useMaplibreUIEffect(
    ({ map }) => {
      layerGroups[0].entries.forEach((entry) => {
        if (entry.type === "source-layer" && entry.subLayers) {
          entry.subLayers.forEach(({ id: layerId }) => {
            if (map.getLayer(layerId)) {
              const visible = map.getLayoutProperty(layerId, "visibility") !== "none";
              if (visible && !selected.includes(entry.id)) {
                map.setLayoutProperty(layerId, "visibility", "none");
              } else if (!visible && selected.includes(entry.id)) {
                map.setLayoutProperty(layerId, "visibility", "visible");
              }
            }
          });
        }
      });
    },
    [selected]
  );

  const isSubLayerOpen = (name) => {
    return open.includes(name);
  };

  /*    Test von entry.id und subLayer.id:
  layerGroups[0].entries.map((entry) => {
    console.log("aaa", entry.id);
    if (entry.subLayers) {
      entry.subLayers.map((subLayer) => {
        console.log("zzzz", subLayer.id);
      });
    }
  });
  */

  return (
    <div
      className="accordion"
      id="layer-control"
      style={{ position: "absolute", zIndex: 1, top: "40px", left: "30px" }}
    >
      {layerGroups[0].entries.map((entry) => (
        <div className="accordion-item" key={entry.id}>
          <h2 className="accordion-header" id={entry.id}>
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
                onSelect(entry);
                onOpen(entry);
              }}
              active={isSubLayerOpen(entry.id)}
              className={`accordion-button ${isSubLayerOpen(entry.id) ? "collapsed" : ""}`}
              type="button"
              data-bs-toggle="collapse"
              data-bs-target={`#collapse-${entry.id}`}
              aria-expanded={isSubLayerOpen(entry.id)}
              aria-controls={`collapse-${entry.id}`}
            >
              <span style={{ marginRight: "10px" }}>{entry.id}</span>
            </button>
          </h2>
          {entry.subLayers
            ? entry.subLayers.map((subLayer) => (
                <Collapse
                  isOpen={isSubLayerOpen(entry.id)}
                  id={`collapse-${subLayer.id}`}
                  key={subLayer.id}
                  className="accordion-collapse"
                  aria-labelledby={`heading-${subLayer.id}`}
                  data-bs-parent="#layer-control"
                >
                  <span style={{ marginLeft: "25px" }}>{subLayer.id}</span>
                </Collapse>
              ))
            : null}
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
