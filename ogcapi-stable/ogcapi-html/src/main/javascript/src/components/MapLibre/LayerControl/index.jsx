import React, { useState } from "react";
import PropTypes from "prop-types";
import { useMaplibreUIEffect } from "react-maplibre-ui";
import { useMapVisibility } from "./fetchStyle";
import Entries from "./EntriesLayer";

const LayerControl = ({ layerGroups }) => {
  const parent = layerGroups.filter((entry) => entry.type === "group");
  const basemaps = layerGroups.filter((entry) => entry.isBasemap === true);
  const visibility = useMapVisibility();

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

  const [layerControlVisible, setLayerControlVisible] = useState(false);
  const [selectedBasemap, setSelectedBasemap] = useState([basemaps[0].entries[0].id]);
  const [selected, setSelected] = useState(subLayerIds);
  const [open, setOpen] = useState([]);

  const handleButtonClick = () => {
    setLayerControlVisible(!layerControlVisible);
  };

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

  return (
    <>
      <div
        className="Button"
        style={{
          backgroundColor: "white",
          position: "absolute",
          zIndex: 1,
          top: "40px",
          left: "30px",
          backgroundColor: "white",
          borderRadius: "8px",
          height: "40px",
        }}
      >
        <button
          style={{
            backgroundColor: "white",
            borderRadius: "0.25rem",
            padding: "10px",
            paddingLeft: "10px",
            height: "40px",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
          }}
          onClick={handleButtonClick}
        >
          Layer Control
        </button>
      </div>
      <div
        className="accordion"
        id="layer-control"
        style={{
          backgroundColor: "white",
          position: "absolute",
          zIndex: 1,
          top: "87px",
          left: "30px",
          width: "275px",
          display: layerControlVisible ? "block" : "none",
          backgroundColor: "white",
          height: "auto",
          maxHeight: "100%",
          overflow: "auto",
          borderRadius: "8px",
          border: "1px solid black",
          scrollbarWidth: "thin",
          scrollbarColor: "darkgrey #f1f1f1",
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
                    paddingLeft: "10px",
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

              <Entries
                p={p}
                isSubLayerOpen={isSubLayerOpen}
                selected={selected}
                selectedBasemap={selectedBasemap}
                setSelected={setSelected}
                setSelectedBasemap={setSelectedBasemap}
                allParentGroups={allParentGroups}
                open={open}
                setOpen={setOpen}
              />
            </div>
          ) : null
        )}
      </div>
    </>
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
