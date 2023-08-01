import React from "react";
import PropTypes from "prop-types";
import { Collapse, Row, Col, FormGroup, Label, Input } from "reactstrap";
import SubLayers from "./SubLayers";
import CollapseButton from "./CollapseButton";
import { LegendSymbolReact } from "./LegendSymbol";

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
  style,
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

  const findLayerIndexById = (layerId) => {
    if (style && style.layers) {
      const allLayerIds = style.layers.map((l) => l.id);
      const index = allLayerIds.indexOf(layerId);
      if (index >= 0) {
        return [index];
      }
    }
    return null;
  };

  return (
    <>
      {allParentGroups
        ? parent.entries.map((entry, i) => {
            const layerIndex = findLayerIndexById(entry.id);
            return (
              <Collapse
                key={entry.id}
                isOpen={isSubLayerOpen(parent.id)}
                id={`collapse-${entry.id}`}
                style={{ paddingBottom: i === parent.entries.length - 1 ? "5px" : null }}
              >
                <Row key={entry.id}>
                  {parent.isBasemap !== true && entry.type === "source-layer" ? (
                    <>
                      <Col xs="10" style={{ display: "flex", alignItems: "center" }}>
                        <FormGroup check style={{ marginLeft: "20px" }}>
                          <Label check>
                            <Input
                              type="checkbox"
                              id={`checkbox-${parent.id}`}
                              checked={entry.subLayers.every((subLayer) =>
                                selected.includes(subLayer.id)
                              )}
                              onChange={(e) => {
                                e.target.blur();
                                onSelectEntry(entry);
                              }}
                            />
                            {entry.id}
                          </Label>
                        </FormGroup>
                      </Col>
                      <Col xs="2">
                        <CollapseButton
                          collapsed={!isSubLayerOpen(entry.id)}
                          toggleCollapsed={() => onOpen(entry)}
                        />
                      </Col>
                    </>
                  ) : (
                    <Col xs="12" style={{ display: "flex", alignItems: "center" }}>
                      <FormGroup check>
                        <Label check style={{ display: "flex", alignItems: "center" }}>
                          <Input
                            style={{
                              position: "relative",
                              marginRight: "5px",
                              marginTop: "0",
                            }}
                            type="radio"
                            name={parent.id}
                            value={`${entry.id}`}
                            checked={selectedBasemap.includes(entry.id)}
                            onChange={(e) => {
                              e.target.blur();
                              onSelectRadio(entry);
                            }}
                          />
                          {style && style.layers && style.layers[layerIndex] && (
                            <LegendSymbolReact
                              style={{
                                display: "inline-block",
                                width: "16px",
                                height: "16px",
                                marginRight: "5px",
                                border: "1px solid #ddd",
                                boxSizing: "content-box",
                              }}
                              sprite={style.sprite}
                              zoom={style.zoom}
                              layer={style.layers[layerIndex]}
                            />
                          )}
                          {entry.id}
                        </Label>
                      </FormGroup>
                    </Col>
                  )}
                </Row>
                <SubLayers
                  layer={entry}
                  isSubLayerOpen={isSubLayerOpen}
                  selected={selected}
                  setSelected={setSelected}
                  allParentGroups={allParentGroups}
                  open={open}
                  style={style}
                />
              </Collapse>
            );
          })
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
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
};

Entries.defaultProps = {};

export default Entries;
