import React from "react";
import PropTypes from "prop-types";
import { Col } from "reactstrap";
import { LegendSymbolReact } from "./LegendSymbol";
import { HeaderCheck } from "./Header";

const Layer = ({ id, label, icons, style, level, radioGroup, isSelected, onSelect }) => {
  const hasFill = icons.some(
    (icon) =>
      style && style.layers && style.layers[icon.index] && style.layers[icon.index].type === "fill"
  );
  const cleanIcons = hasFill
    ? icons.filter(
        (icon) =>
          style &&
          style.layers &&
          style.layers[icon.index] &&
          style.layers[icon.index].type !== "line"
      )
    : icons;

  return (
    <Col xs="12" style={{ display: "flex", alignItems: "center" }}>
      <HeaderCheck
        id={id}
        level={level}
        radioGroup={radioGroup}
        isSelected={isSelected}
        onSelect={onSelect}
      >
        <div
          style={{
            position: "relative",
            width: "16px",
            height: "16px",
            marginRight: "5px",
            border: "1px solid #ddd",
            boxSizing: "content-box",
          }}
        >
          {style &&
            style.layers &&
            cleanIcons.map(
              (icon) =>
                style.layers[icon.index] && (
                  <LegendSymbolReact
                    key={icon.id}
                    style={{
                      position: "absolute",
                      top: 0,
                      left: 0,
                    }}
                    sprite={style.sprite}
                    zoom={icon.zoom || style.zoom}
                    layer={style.layers[icon.index]}
                    properties={icon.properties}
                  />
                )
            )}
        </div>
        <span style={{ whiteSpace: "nowrap" }}>{label || id}</span>
      </HeaderCheck>
    </Col>
  );
};

Layer.displayName = "Layer";

Layer.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string,
  icons: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.object])).isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
  level: PropTypes.number,
  radioGroup: PropTypes.string,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
};

Layer.defaultProps = {
  label: undefined,
  level: 0,
  radioGroup: undefined,
};

export default Layer;
