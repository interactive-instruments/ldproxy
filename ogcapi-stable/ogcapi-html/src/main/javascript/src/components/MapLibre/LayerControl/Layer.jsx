import React from "react";
import PropTypes from "prop-types";
import { Col, FormGroup, Label, Input } from "reactstrap";
import { LegendSymbolReact } from "./LegendSymbol";

const findLayerIndexById = (style, layerId) => {
  if (style && style.layers) {
    const allLayerIds = style.layers.map((l) => l.id);
    const index = allLayerIds.indexOf(layerId);
    if (index >= 0) {
      return [index];
    }
  }
  return null;
};

const Layer = ({ layer, style, level, radioGroup, isSelected, onSelect }) => {
  const layerIndex = findLayerIndexById(style, layer.layers ? layer.layers[0] : layer.id);

  return (
    <Col xs="12" style={{ display: "flex", alignItems: "center" }}>
      <FormGroup check style={{ marginLeft: `${level * 20}px` }}>
        <Label check style={{ display: "flex", alignItems: "center" }}>
          <Input
            style={{
              position: "relative",
              marginRight: "5px",
              marginTop: "0",
            }}
            type={radioGroup ? "radio" : "checkbox"}
            name={radioGroup}
            value={layer.id}
            checked={isSelected(layer.id, radioGroup)}
            onChange={() => onSelect(layer.id, radioGroup)}
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
              zoom={layer.zoom || style.zoom}
              layer={style.layers[layerIndex]}
              properties={layer.properties}
            />
          )}
          <span style={{ whiteSpace: "nowrap" }}>{layer.id}</span>
        </Label>
      </FormGroup>
    </Col>
  );
};

Layer.displayName = "Layer";

Layer.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  layer: PropTypes.object.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
  level: PropTypes.number,
  radioGroup: PropTypes.string,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
};

Layer.defaultProps = {
  level: 0,
  radioGroup: undefined,
};

export default Layer;
