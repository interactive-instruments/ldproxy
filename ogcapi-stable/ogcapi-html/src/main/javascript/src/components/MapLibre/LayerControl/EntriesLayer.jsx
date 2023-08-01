import React from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";
import SubLayers from "./SubLayers";

const Entries = ({
  parent,
  isSubLayerOpen,
  selected,
  selectedBasemap,
  setSelected,
  setSelectedBasemap,
  allParentGroups,
  open,
  setOpen,
}) => {
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

  const onSelectRadio = (entry) => {
    if (!selectedBasemap.includes(entry.id)) {
      setSelectedBasemap(null);
      setSelectedBasemap([entry.id]);
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

  return (
    <>
      {allParentGroups
        ? parent.entries.map((entry) => (
            <div key={entry.id}>
              <Collapse
                isOpen={isSubLayerOpen(parent.id)}
                id={`collapse-${entry.id}`}
                key={entry.id}
                className="accordion-collapse"
                aria-labelledby={`heading-${entry.id}`}
                data-bs-parent="#layer-control"
              >
                {parent.isBasemap !== true ? (
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
                      className={`accordion-button ${isSubLayerOpen(entry.id) ? "collapsed" : ""}`}
                      type="button"
                      data-bs-toggle="collapse"
                      data-bs-target={`#collapse-${entry.id}`}
                      aria-expanded={isSubLayerOpen(entry.id)}
                      aria-controls={`collapse-${entry.id}`}
                    >
                      <input
                        style={{ marginLeft: "25px" }}
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

                      <span style={{ marginRight: "5px", marginLeft: "5px" }}>{entry.id}</span>
                    </button>
                  </div>
                ) : (
                  <div>
                    <input
                      style={{ marginRight: "10px", marginLeft: "15px" }}
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
              <SubLayers
                layer={entry}
                isSubLayerOpen={isSubLayerOpen}
                selected={selected}
                setSelected={setSelected}
                allParentGroups={allParentGroups}
                open={open}
              />
            </div>
          ))
        : null}
    </>
  );
};

Entries.displayName = "Entries";

Entries.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  parent: PropTypes.object.isRequired,
  isSubLayerOpen: PropTypes.func.isRequired,
  selected: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectedBasemap: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelected: PropTypes.func.isRequired,
  setSelectedBasemap: PropTypes.func.isRequired,
  allParentGroups: PropTypes.arrayOf(PropTypes.object).isRequired,
  open: PropTypes.arrayOf(PropTypes.string).isRequired,
  setOpen: PropTypes.func.isRequired,
};

Entries.defaultProps = {};

export default Entries;
