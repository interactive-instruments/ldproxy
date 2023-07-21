import React, { useState } from "react";
import PropTypes from "prop-types";
import { useMaplibreUIEffect } from "react-maplibre-ui";
import { useMapVisibility } from "./fetchStyle";
import ParentGroups from "./ParentGroups";

const LayerControl = ({ layerGroups }) => {
  const parent = layerGroups.filter((entry) => entry.type === "group");
  const basemaps = layerGroups.filter((entry) => entry.isBasemap === true);
  const visibility = useMapVisibility();
  if (visibility !== null) {
    console.log("vis", visibility);
  }

  const allParentGroups = parent.flatMap((p) => {
    return p.entries.map((value) => {
      return value;
    });
  });

  const allSubLayers = allParentGroups.map((group) => {
    return group.subLayers && group.subLayers.length > 0
      ? group.subLayers.map((subLayer) => {
          return subLayer.id;
        })
      : [];
  });

  const subLayerIds = allSubLayers.flatMap((Ids) => {
    return Ids;
  });

  const [layerControlVisible, setLayerControlVisible] = useState(false);
  const [selectedBasemap, setSelectedBasemap] = useState([basemaps[0].entries[0].id]);
  const [selected, setSelected] = useState(subLayerIds);
  const [open, setOpen] = useState([]);

  const handleButtonClick = () => {
    setLayerControlVisible(!layerControlVisible);
  };

  useMaplibreUIEffect(({ map }) => {
    allParentGroups.forEach((entry) => {
      if (entry.type === "source-layer" && entry.subLayers) {
        entry.subLayers.forEach(({ id: layerId }) => {
          if (map.getLayer(layerId)) {
            const visible = map.getLayoutProperty(layerId, "visibility") !== "none";
            if (visible && !selected.includes(layerId)) {
              map.setLayoutProperty(layerId, "visibility", "none");
            } else if (!visible && selected.includes(layerId)) {
              map.setLayoutProperty(layerId, "visibility", "visible");
            }
          }
        });
      } else {
        if (map.getLayer(entry.id)) {
          const visible = map.getLayoutProperty(entry.id, "visibility") !== "none";
          if (visible && !selectedBasemap.includes(entry.id)) {
            map.setLayoutProperty(entry.id, "visibility", "none");
          } else if (!visible && selectedBasemap.includes(entry.id)) {
            map.setLayoutProperty(entry.id, "visibility", "visible");
          }
        }
      }
    });
  }, [selected] + [selectedBasemap]);

  const isSubLayerOpen = (name) => {
    return open.includes(name);
  };

  return (
    <>
      <div
        className="Button"
        style={{
          backgroundColor: "white",
          position: "absolute",
          zIndex: 1,
          top: "40px",
          left: "30px",
          backgroundColor: "white",
          borderRadius: "8px",
          height: "40px",
        }}
      >
        <button
          style={{
            backgroundColor: "white",
            borderRadius: "0.25rem",
            padding: "10px",
            paddingLeft: "10px",
            height: "40px",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
          }}
          onClick={handleButtonClick}
        >
          Layer Control
        </button>
      </div>

      <ParentGroups
        layerControlVisible={layerControlVisible}
        parent={parent}
        isSubLayerOpen={isSubLayerOpen}
        selected={selected}
        selectedBasemap={selectedBasemap}
        setSelected={setSelected}
        setSelectedBasemap={setSelectedBasemap}
        allParentGroups={allParentGroups}
        open={open}
        setOpen={setOpen}
        subLayerIds={subLayerIds}
      />
    </>
  );
};

LayerControl.displayName = "LayerControl";

LayerControl.propTypes = {
  layerGroups: PropTypes.objectOf(PropTypes.array),
};

LayerControl.defaultProps = {
  layerGroups: {},
};

export default LayerControl;
