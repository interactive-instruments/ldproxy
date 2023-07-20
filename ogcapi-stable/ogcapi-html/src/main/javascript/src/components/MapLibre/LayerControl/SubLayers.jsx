import React, { useState } from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";

const SubLayers = ({ entry, isSubLayerOpen, selected, setSelected }) => {
  const onSelect = (entry) => {
    const index = selected.indexOf(entry.id);
    if (index < 0) {
      selected.push(entry.id);
    } else {
      selected.splice(index, 1);
    }
    setSelected([...selected]);
  };

  return (
    <>
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
    </>
  );
};

export default SubLayers;
