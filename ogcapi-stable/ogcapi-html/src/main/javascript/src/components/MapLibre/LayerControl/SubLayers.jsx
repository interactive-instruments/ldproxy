import React from "react";
import PropTypes from "prop-types";
import { Collapse, Row, Col, FormGroup, Label, Input } from "reactstrap";
import { LegendSymbolReact } from "./LegendSymbol";

const SubLayers = ({ layer, isSubLayerOpen, selected, setSelected, style }) => {
  const onSelect = (entry) => {
    const index = selected.indexOf(entry.id);
    if (index < 0) {
      selected.push(entry.id);
    } else {
      selected.splice(index, 1);
    }
    setSelected([...selected]);
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
      {layer.subLayers && layer.subLayers.length > 0
        ? layer.subLayers.map((subLayer, i) => {
            const layerIndex = findLayerIndexById(subLayer.id);

            return (
              <Collapse
                isOpen={isSubLayerOpen(layer.id)}
                id={`collapse-${subLayer.id}`}
                key={subLayer.id}
                style={{ paddingBottom: i === layer.subLayers.length - 1 ? "5px" : null }}
              >
                <Row key={subLayer.id}>
                  <Col xs="12" style={{ display: "flex", alignItems: "center" }}>
                    <FormGroup check style={{ marginLeft: "40px" }}>
                      <Label check>
                        <Input
                          type="checkbox"
                          id={`checkbox-${subLayer.id}`}
                          checked={selected.includes(subLayer.id)}
                          onChange={() => onSelect(subLayer)}
                        />
                        {style && style.layers && style.layers[layerIndex] && (
                          <LegendSymbolReact
                            style={{
                              width: "16px",
                              height: "16px",
                              marginRight: "5px",
                              border: "1px solid #ddd",
                              boxSizing: "content-box",
                            }}
                            sprite={style.sprite}
                            zoom={subLayer.zoom || style.zoom}
                            layer={style.layers[layerIndex]}
                            properties={subLayer.properties}
                          />
                        )}
                        {subLayer.id}
                      </Label>
                    </FormGroup>
                  </Col>
                </Row>
              </Collapse>
            );
          })
        : null}
    </>
  );
};

SubLayers.displayName = "SubLayers";

SubLayers.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  layer: PropTypes.object.isRequired,
  isSubLayerOpen: PropTypes.func.isRequired,
  selected: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelected: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
};

SubLayers.defaultProps = {};

export default SubLayers;
