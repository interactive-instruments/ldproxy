import React from "react";
import PropTypes from "prop-types";
import Entries from "./EntriesLayer";

const ParentGroups = ({
  layerControlVisible,
  parents,
  isSubLayerOpen,
  selected,
  selectedBasemap,
  setSelected,
  setSelectedBasemap,
  allParentGroups,
  open,
  setOpen,
  subLayerIds,
}) => {
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
      const subLayerIds2 = entry.entries.flatMap((e) => e.subLayers.map((subLayer) => subLayer.id));
      const idsToRemove = [...entryIds, ...subLayerIds2];

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

  // map height: 383px

  return (
    <>
      <div
        className="accordion"
        id="layer-control"
        style={{
          position: "absolute",
          zIndex: 1,
          top: "87px",
          left: "30px",
          width: "350px",
          display: layerControlVisible ? "block" : "none",
          backgroundColor: "white",
          height: "auto",
          maxHeight: "65%",
          overflow: "auto",
          borderRadius: "8px",
          border: "1px solid black",
          scrollbarWidth: "thin",
          scrollbarColor: "darkgrey #f1f1f1",
        }}
      >
        {parents.map((parent) =>
          parent.id ? (
            <div className="accordion-item" key={parent.id}>
              <h2 className="accordion-header" id={parent.id}>
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
                      onOpenParent(parent);
                    }
                  }}
                  active={isSubLayerOpen(parent.id)}
                  className={`accordion-button ${isSubLayerOpen(parent.id) ? "collapsed" : ""}`}
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target={`#collapse-${parent.id}`}
                  aria-expanded={isSubLayerOpen(parent.id)}
                  aria-controls={`collapse-${parent.id}`}
                >
                  {parent.isBasemap !== true ? (
                    <>
                      <input
                        style={{ margin: "5px" }}
                        className="form-check-input"
                        type="checkbox"
                        id={`checkbox-${parent.id}`}
                        checked={parentCheck()}
                        onChange={(e) => {
                          e.target.blur();
                          onSelectParent();
                        }}
                      />
                      <span style={{ marginRight: "10px" }}>{parent.id}</span>
                    </>
                  ) : (
                    <span style={{ marginLeft: "5px", marginRight: "5px" }}>{parent.id}</span>
                  )}
                </button>
              </h2>
              <Entries
                parent={parent}
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

ParentGroups.displayName = "ParentGroups";

ParentGroups.propTypes = {
  layerControlVisible: PropTypes.bool.isRequired,
  parents: PropTypes.arrayOf(PropTypes.object).isRequired,
  isSubLayerOpen: PropTypes.func.isRequired,
  selected: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectedBasemap: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelected: PropTypes.func.isRequired,
  setSelectedBasemap: PropTypes.func.isRequired,
  allParentGroups: PropTypes.arrayOf(PropTypes.object).isRequired,
  open: PropTypes.arrayOf(PropTypes.string).isRequired,
  setOpen: PropTypes.func.isRequired,
  subLayerIds: PropTypes.arrayOf(PropTypes.string).isRequired,
};

ParentGroups.defaultProps = {};

export default ParentGroups;
