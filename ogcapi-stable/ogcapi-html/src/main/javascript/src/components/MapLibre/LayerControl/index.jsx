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

  const onSelectParent = () => {
    if (!layerGroups[0].entries.every((entry) => selected.includes(entry.id))) {
      const allEntries = layerGroups[0].entries.map((entry) => entry.id);
      setSelected(allEntries);
    } else {
      setSelected([]);
    }
  };

  const onSelect = (entry) => {
    const index = selected.indexOf(entry.id);
    if (index < 0) {
      selected.push(entry.id);
    } else {
      selected.splice(index, 1);
    }
    setSelected([...selected]);
  };

  const onOpenParent = (entry) => {
    const index = open.indexOf(entry.id);
    if (index < 0) {
      open.push(entry.id);
      setOpen([...open]);
    } else {
      setOpen([]);
    }
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

  //   Test von entry.id und subLayer.id:
  /*  layerGroups[0].entries.map((entry) => {
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
      style={{
        backgroundColor: "white",
        position: "absolute",
        zIndex: 1,
        top: "40px",
        left: "30px",
      }}
    >
      {layerGroups[0].id ? (
        <div className="accordion-item" key={layerGroups[0].id}>
          <h2 className="accordion-header" id={layerGroups[0].id}>
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
                if (!e.target.classList.contains("form-check-input")) {
                  e.target.blur();
                  onOpenParent(layerGroups[0]);
                }
              }}
              active={isSubLayerOpen(layerGroups[0].id)}
              className={`accordion-button ${isSubLayerOpen(layerGroups[0].id) ? "collapsed" : ""}`}
              type="button"
              data-bs-toggle="collapse"
              data-bs-target={`#collapse-${layerGroups[0].id}`}
              aria-expanded={isSubLayerOpen(layerGroups[0].id)}
              aria-controls={`collapse-${layerGroups[0].id}`}
            >
              <input
                style={{ margin: "5px" }}
                className="form-check-input"
                type="checkbox"
                id={`checkbox-${layerGroups[0].id}`}
                checked={layerGroups[0].entries.every((entry) => selected.includes(entry.id))}
                onChange={(e) => {
                  e.target.blur();
                  onSelectParent();
                }}
              />
              <span style={{ marginRight: "10px" }}>{layerGroups[0].id}</span>
            </button>
          </h2>
        </div>
      ) : null}
      {layerGroups[0].entries
        ? layerGroups[0].entries.map((entry) => (
            <div key={entry.id}>
              <Collapse
                isOpen={isSubLayerOpen(layerGroups[0].id)}
                id={`collapse-${entry.id}`}
                key={entry.id}
                className="accordion-collapse"
                aria-labelledby={`heading-${entry.id}`}
                data-bs-parent="#layer-control"
              >
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
                    if (!e.target.classList.contains("form-check-input")) {
                      e.target.blur();
                      onOpen(entry);
                    }
                  }}
                  active={isSubLayerOpen(entry.id)}
                  className={`accordion-button ${isSubLayerOpen(entry.id) ? "collapsed" : ""}`}
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target={`#collapse-${entry.id}`}
                  aria-expanded={isSubLayerOpen(entry.id)}
                  aria-controls={`collapse-${entry.id}`}
                >
                  <input
                    style={{ margin: "5px" }}
                    className="form-check-input"
                    type="checkbox"
                    id={`checkbox-${entry.id}`}
                    checked={selected.includes(entry.id)}
                    onChange={(e) => {
                      e.target.blur();
                      onSelect(entry);
                    }}
                  />
                  <span style={{ marginRight: "10px" }}>{entry.id}</span>
                </button>
              </Collapse>

              {entry.subLayers
                ? entry.subLayers.map((subLayer) => (
                    <div key={subLayer.id}>
                      <Collapse
                        isOpen={isSubLayerOpen(entry.id)}
                        id={`collapse-${subLayer.id}`}
                        key={subLayer.id}
                        className="accordion-collapse"
                        aria-labelledby={`heading-${subLayer.id}`}
                        data-bs-parent="#layer-control"
                      >
                        <input
                          style={{ marginLeft: "5px" }}
                          className="form-check-input"
                          type="checkbox"
                          id={`checkbox-${subLayer.id}`}
                          checked={selected.includes(subLayer.id)}
                          onChange={() => onSelect(subLayer)}
                        />
                        <span style={{ marginLeft: "10px" }}>{subLayer.id}</span>
                      </Collapse>
                    </div>
                  ))
                : null}
            </div>
          ))
        : null}
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
