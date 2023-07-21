import React, { useState } from "react";
import PropTypes from "prop-types";
import { Collapse } from "reactstrap";
import { LegendSymbolReact } from "./LegendSymbol";
import { useMaplibreUIEffect } from "react-maplibre-ui";

const SubLayers = ({ entry, isSubLayerOpen, selected, setSelected, allParentGroups, open }) => {
  const [style, setStyle] = useState();
  const [sprite, setSprite] = useState("https://demo.ldproxy.net/daraa/resources/sprites");
  const [zoom, setZoom] = useState(12);

  useMaplibreUIEffect(
    ({ map }) => {
      allParentGroups.forEach((entry) => {
        if (entry.type === "source-layer" && entry.subLayers) {
          entry.subLayers.forEach(({ id: layerId }) => {
            if (map.getLayer(layerId)) {
              const style = map.getStyle();
              const sprite = style.sprite;
              const zoom = style.zoom;
              setStyle(style);
              setSprite(sprite);
              setZoom(zoom);
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
      const allLayerIds = style.layers.map((layer) => layer.id);
      const index = allLayerIds.indexOf(layerId);
      if (index >= 0) {
        return [index];
      }
    }
  };

  return (
    <>
      {entry.subLayers && entry.subLayers.length > 0
        ? entry.subLayers.map((subLayer) => {
            const layerIndex = findLayerIndexById(subLayer.id);
            console.log("layerIndex", `style.layers[${layerIndex}]`);
            return (
              <div key={subLayer.id}>
                <Collapse
                  isOpen={isSubLayerOpen(entry.id)}
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

export default SubLayers;
