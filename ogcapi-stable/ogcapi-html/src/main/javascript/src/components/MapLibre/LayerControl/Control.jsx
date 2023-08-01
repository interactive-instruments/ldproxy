import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import ParentGroups from "./ParentGroups";

const Control = ({ layerGroups, mapHeight, map }) => {
  const parents = layerGroups.filter((entry) => entry.type === "group");
  const basemaps = layerGroups.filter((entry) => entry.isBasemap === true);

  const allParentGroups = parents.flatMap((p) => {
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

  const [layerControlVisible, setLayerControlVisible] = useState(true);
  const [selectedBasemap, setSelectedBasemap] = useState([basemaps[0].entries[0].id]);
  const [selected, setSelected] = useState(subLayerIds);
  const [open, setOpen] = useState(["Basemap", "All", "TransportationGroundCrv", "Test"]); // (allParentGroups.map((p) => p.id) + basemaps.map((p) => p.id));
  const [style, setStyle] = useState();

  const handleButtonClick = () => {
    setLayerControlVisible(!layerControlVisible);
  };

  useEffect(() => {
    map.on("style.load", () => {
      setStyle(map.getStyle());
    });
  }, [map, setStyle]);

  useEffect(() => {
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
      } else if (map.getLayer(entry.id)) {
        const visible = map.getLayoutProperty(entry.id, "visibility") !== "none";
        if (visible && !selectedBasemap.includes(entry.id)) {
          map.setLayoutProperty(entry.id, "visibility", "none");
        } else if (!visible && selectedBasemap.includes(entry.id)) {
          map.setLayoutProperty(entry.id, "visibility", "visible");
        }
      }
    });
  }, [map, allParentGroups, selected, selectedBasemap]);

  const isSubLayerOpen = (name) => {
    return open.includes(name);
  };

  return (
    <>
      <div className="maplibregl-ctrl maplibregl-ctrl-group" style={{}}>
        <button
          type="button"
          style={{
            backgroundColor: layerControlVisible ? "rgb(0 0 0/5%)" : "transparent",
          }}
          onClick={handleButtonClick}
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            fill="currentColor"
            viewBox="0 0 16 16"
          >
            <path d="M8.235 1.559a.5.5 0 0 0-.47 0l-7.5 4a.5.5 0 0 0 0 .882L3.188 8 .264 9.559a.5.5 0 0 0 0 .882l7.5 4a.5.5 0 0 0 .47 0l7.5-4a.5.5 0 0 0 0-.882L12.813 8l2.922-1.559a.5.5 0 0 0 0-.882l-7.5-4zm3.515 7.008L14.438 10 8 13.433 1.562 10 4.25 8.567l3.515 1.874a.5.5 0 0 0 .47 0l3.515-1.874zM8 9.433 1.562 6 8 2.567 14.438 6 8 9.433z" />
          </svg>
        </button>
      </div>

      <ParentGroups
        layerControlVisible={layerControlVisible}
        parents={parents}
        isSubLayerOpen={isSubLayerOpen}
        selected={selected}
        selectedBasemap={selectedBasemap}
        setSelected={setSelected}
        setSelectedBasemap={setSelectedBasemap}
        allParentGroups={allParentGroups}
        open={open}
        setOpen={setOpen}
        subLayerIds={subLayerIds}
        mapHeight={mapHeight}
        style={style}
      />
    </>
  );
};

Control.displayName = "Control";

Control.propTypes = {
  layerGroups: PropTypes.arrayOf(PropTypes.object),
  mapHeight: PropTypes.number.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  map: PropTypes.object.isRequired,
};

Control.defaultProps = {
  layerGroups: [],
};

export default Control;
