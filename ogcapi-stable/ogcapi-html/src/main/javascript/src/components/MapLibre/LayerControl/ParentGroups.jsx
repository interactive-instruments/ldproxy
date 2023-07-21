import React, { useState } from "react";
import PropTypes from "prop-types";
import Entries from "./EntriesLayer";

const ParentGroups = ({
  layerControlVisible,
  parent,
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

  // map height: 383px

  return (
    <>
      <div
        className="accordion"
        id="layer-control"
        style={{
          backgroundColor: "white",
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
        {parent.map((p) =>
          p.id ? (
            <div className="accordion-item" key={p.id}>
              <h2 className="accordion-header" id={p.id}>
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
                    <>
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
                      <span style={{ marginRight: "10px" }}>{p.id}</span>
                    </>
                  ) : (
                    <span style={{ marginLeft: "5px", marginRight: "5px" }}>{p.id}</span>
                  )}
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

export default ParentGroups;
