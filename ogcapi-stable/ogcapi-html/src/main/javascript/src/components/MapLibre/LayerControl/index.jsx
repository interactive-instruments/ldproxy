import React, { useState } from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";
import { useMaplibreUIEffect } from "react-maplibre-ui";

const LayerControl = ({ layerGroups }) => {
  const parent = layerGroups.filter((entry) => entry.type === "group");
  const basemaps = layerGroups.filter((entry) => entry.isBasemap === true);

  const allParentGroups = parent.flatMap((p) => {
    return p.entries.map((value) => {
      return value;
    });
  });

  const allSubLayers = allParentGroups.map((group) => {
    return group.subLayers && group.subLayers.length > 0
      ? group.subLayers.map((subLayer) => {
          return subLayer.id;
        })
      : [];
  });

  const subLayerIds = allSubLayers.flatMap((Ids) => {
    return Ids;
  });

  const [selectedBasemap, setSelectedBasemap] = useState([basemaps[0].entries[0].id]);
  const [selected, setSelected] = useState(subLayerIds);
  const [open, setOpen] = useState([]);

  const onSelectParent = () => {
    if (selected.every((id) => !subLayerIds.includes(id))) {
      setSelected([...selected, ...subLayerIds]);
    } else {
      setSelected(selected.filter((id) => !subLayerIds.includes(id)));
    }
  };

  const parentCheck = () => {
    return subLayerIds.every((id) => selected.includes(id));
  };

  const onSelectEntry = (entry) => {
    const subLayers = entry.subLayers.map((subLayer) => {
      return subLayer.id;
    });
    if (subLayers.every((id) => selected.includes(id))) {
      setSelected(selected.filter((id) => !subLayers.includes(id)));
    } else {
      setSelected([...selected, ...subLayers]);
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

  const onSelectRadio = (entry) => {
    if (!selectedBasemap.includes(entry.id)) {
      setSelectedBasemap(null);
      setSelectedBasemap([entry.id]);
    }
  };

  const onOpenParent = (entry) => {
    if (entry.isBasemap !== true) {
      const entryIds = [entry.id, ...entry.entries.map((e) => e.id)];
      const subLayerIds = entry.entries.flatMap((e) => e.subLayers.map((subLayer) => subLayer.id));
      const idsToRemove = [...entryIds, ...subLayerIds];

      const index = open.indexOf(entry.id);
      if (index < 0) {
        open.push(entry.id);
        setOpen([...open]);
      } else {
        setOpen(open.filter((ids) => !idsToRemove.includes(ids)));
      }
    } else {
      const index = open.indexOf(entry.id);
      if (index < 0) {
        open.push(entry.id);
        setOpen([...open]);
      } else {
        setOpen(open.filter((ids) => !entry.id.includes(ids)));
      }
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

  useMaplibreUIEffect(({ map }) => {
    allParentGroups.forEach((entry) => {
      if (entry.type === "source-layer" && entry.subLayers) {
        entry.subLayers.forEach(({ id: layerId }) => {
          if (map.getLayer(layerId)) {
            const visible = map.getLayoutProperty(layerId, "visibility") !== "none";
            if (visible && !selected.includes(layerId)) {
              map.setLayoutProperty(layerId, "visibility", "none");
            } else if (!visible && selected.includes(layerId)) {
              map.setLayoutProperty(layerId, "visibility", "visible");
            }
          }
        });
      } else {
        if (map.getLayer(entry.id)) {
          const visible = map.getLayoutProperty(entry.id, "visibility") !== "none";
          if (visible && !selectedBasemap.includes(entry.id)) {
            map.setLayoutProperty(entry.id, "visibility", "none");
          } else if (!visible && selectedBasemap.includes(entry.id)) {
            map.setLayoutProperty(entry.id, "visibility", "visible");
          }
        }
      }
    });
  }, [selected] + [selectedBasemap]);

  const isSubLayerOpen = (name) => {
    return open.includes(name);
  };
  /*
  //   Test von allParentGroups und parent.id:
  console.log("allPGroups", allParentGroups);
  console.log(
    "parent",
    parent.map((p) => {
      return p;
    })
  );
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
      {parent.map((p) =>
        p.id ? (
          <div className="accordion-item" key={p.id}>
            <h2 className="accordion-header" id={p.id}>
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
                    onOpenParent(p);
                  }
                }}
                active={isSubLayerOpen(p.id)}
                className={`accordion-button ${isSubLayerOpen(p.id) ? "collapsed" : ""}`}
                type="button"
                data-bs-toggle="collapse"
                data-bs-target={`#collapse-${p.id}`}
                aria-expanded={isSubLayerOpen(p.id)}
                aria-controls={`collapse-${p.id}`}
              >
                {p.isBasemap !== true ? (
                  <input
                    style={{ margin: "5px" }}
                    className="form-check-input"
                    type="checkbox"
                    id={`checkbox-${p.id}`}
                    checked={parentCheck()}
                    onChange={(e) => {
                      e.target.blur();
                      onSelectParent();
                    }}
                  />
                ) : null}
                <span style={{ marginRight: "10px" }}>{p.id}</span>
              </button>
            </h2>

            {allParentGroups
              ? p.entries.map((entry) => (
                  <div key={entry.id}>
                    <Collapse
                      isOpen={isSubLayerOpen(p.id)}
                      id={`collapse-${entry.id}`}
                      key={entry.id}
                      className="accordion-collapse"
                      aria-labelledby={`heading-${entry.id}`}
                      data-bs-parent="#layer-control"
                    >
                      {p.isBasemap !== true ? (
                        <div>
                          <button
                            style={{
                              backgroundColor: "white",
                              borderRadius: "0.25rem",
                              padding: "10px",
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
                            className={`accordion-button ${
                              isSubLayerOpen(entry.id) ? "collapsed" : ""
                            }`}
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
                              checked={entry.subLayers.every((subLayer) =>
                                selected.includes(subLayer.id)
                              )}
                              onChange={(e) => {
                                e.target.blur();
                                onSelectEntry(entry);
                              }}
                            />

                            <span style={{ marginRight: "10px" }}>{entry.id}</span>
                          </button>
                        </div>
                      ) : (
                        <div>
                          <input
                            style={{ margin: "5px" }}
                            className="form-check-input"
                            type="radio"
                            id={`radiobutton-${entry.id}`}
                            name="basemap"
                            value={`${entry.id}`}
                            checked={selectedBasemap.includes(entry.id)}
                            onClick={(e) => {
                              e.target.blur();
                              onSelectRadio(entry);
                            }}
                          />
                          <span style={{ marginRight: "10px" }}>{entry.id}</span>
                        </div>
                      )}
                    </Collapse>

                    {entry.subLayers && entry.subLayers.length > 0
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
                                style={{ margin: "5px" }}
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
        ) : null
      )}
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
