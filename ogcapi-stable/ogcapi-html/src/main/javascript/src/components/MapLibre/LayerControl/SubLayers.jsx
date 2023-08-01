import React, { useState } from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";
import { useMaplibreUIEffect } from "react-maplibre-ui";
import { LegendSymbolReact } from "./LegendSymbol";

const SubLayers = ({ layer, isSubLayerOpen, selected, setSelected, allParentGroups, open }) => {
  const [style, setStyle] = useState();
  const [sprite, setSprite] = useState("https://demo.ldproxy.net/daraa/resources/sprites");
  const [zoom, setZoom] = useState(12);

  useMaplibreUIEffect(
    ({ map }) => {
      allParentGroups.forEach((group) => {
        if (group.type === "source-layer" && group.subLayers) {
          group.subLayers.forEach(({ id: layerId }) => {
            if (map.getLayer(layerId)) {
              const style2 = map.getStyle();
              const sprite2 = style2.sprite;
              const zoom2 = style2.zoom;
              setStyle(style2);
              setSprite(sprite2);
              setZoom(zoom2);
            }
          });
        }
      });
    },
    [open]
  );

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
        ? layer.subLayers.map((subLayer) => {
            const layerIndex = findLayerIndexById(subLayer.id);
            // console.log("layerIndex", `style.layers[${layerIndex}]`);
            return (
              <div key={subLayer.id}>
                <Collapse
                  isOpen={isSubLayerOpen(layer.id)}
                  id={`collapse-${subLayer.id}`}
                  key={subLayer.id}
                  className="accordion-collapse"
                  aria-labelledby={`heading-${subLayer.id}`}
                  data-bs-parent="#layer-control"
                >
                  <span
                    style={{
                      display: "flex",
                      alignItems: "center",
                      marginLeft: "5px",
                      marginRight: "5px",
                    }}
                  >
                    <input
                      style={{ marginLeft: "55px", marginRight: "5px" }}
                      className="form-check-input"
                      type="checkbox"
                      id={`checkbox-${subLayer.id}`}
                      checked={selected.includes(subLayer.id)}
                      onChange={() => onSelect(subLayer)}
                    />
                    <div style={{ height: "25px", width: "15px", marginRight: "5px" }}>
                      {style && zoom && layerIndex ? (
                        <LegendSymbolReact
                          sprite={sprite}
                          zoom={zoom}
                          layer={style.layers[layerIndex]}
                        />
                      ) : null}
                    </div>
                    {subLayer.id}
                  </span>
                </Collapse>
              </div>
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
  allParentGroups: PropTypes.arrayOf(PropTypes.object).isRequired,
  open: PropTypes.arrayOf(PropTypes.string).isRequired,
};

SubLayers.defaultProps = {};

export default SubLayers;
