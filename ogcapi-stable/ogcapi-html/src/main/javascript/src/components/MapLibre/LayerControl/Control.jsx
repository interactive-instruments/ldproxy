import React, { useState, useEffect, useCallback } from "react";
import PropTypes from "prop-types";
import ControlPanel from "./ControlPanel";
import ControlButton from "./ControlButton";
import { getRadioGroups, getSubLayerIds, getDeps, getParentDeps, getChildDeps } from "./config";

const Control = ({ layerGroups: layerGroupsDefault, mapHeight, map }) => {
  const [parents, setParents] = useState([]);
  const [radioGroups, setRadioGroups] = useState([]);
  const [subLayerIds, setSubLayerIds] = useState([]);
  const [layerControlVisible, setLayerControlVisible] = useState(false);
  const [selectedRadioGroups, setSelectedRadioGroups] = useState({});
  const [selected, setSelected] = useState([]);
  const [open, setOpen] = useState([]);
  const [style, setStyle] = useState();
  const [deps, setDeps] = useState({});

  const onOpen = (id) => {
    if (open.includes(id)) {
      setOpen(open.filter((id2) => id2 !== id));
    } else {
      setOpen([...open, id]);
    }
  };

  const isOpened = (name) => {
    return open.includes(name);
  };

  const onSelect = (id, radioGroup) => {
    if (radioGroup) {
      if (selectedRadioGroups[radioGroup] !== id) {
        setSelectedRadioGroups({ ...selectedRadioGroups, [radioGroup]: id });
      }
      return;
    }
    if (selected.includes(id)) {
      setSelected(selected.filter((id2) => !deps.all[id].includes(id2)));
    } else {
      const newSelected = [...selected, id];

      deps.child[id].forEach((child) => {
        if (!newSelected.includes(child)) {
          newSelected.push(child);
        }
      });

      deps.parent[id].forEach((parent) => {
        if (
          !newSelected.includes(parent) &&
          deps.child[parent].every((child) => newSelected.includes(child))
        ) {
          newSelected.push(parent);
        }
      });

      setSelected(newSelected);
    }
  };

  const isSelected = useCallback(
    (id, radioGroup) =>
      radioGroup ? selectedRadioGroups[radioGroup] === id : selected.includes(id),
    [selected, selectedRadioGroups]
  );

  const toggleLayerControlVisible = () => {
    setLayerControlVisible(!layerControlVisible);
  };

  // initialize state from configuration when style is loaded
  useEffect(() => {
    map.on("style.load", () => {
      const s = map.getStyle();
      const layerGroups =
        s.metadata && s.metadata["ldproxy:layers"]
          ? s.metadata["ldproxy:layers"]
          : layerGroupsDefault;
      const p = layerGroups.filter(
        (entry) => entry.type === "group" || entry.type === "source-layer"
      );
      const r = getRadioGroups(layerGroups, true);
      const r2 = getRadioGroups(layerGroups);
      const a = p.flatMap((parent) =>
        [parent].concat(
          parent.entries
            ? parent.entries.filter(
                (entry) => entry.type === "group" || entry.type === "source-layer"
              )
            : []
        )
      );
      const ids = getSubLayerIds(a);
      const d = getDeps(layerGroups);
      const c = getChildDeps(layerGroups);
      const dp = getParentDeps(layerGroups);

      setStyle(s);
      setParents(p);
      setRadioGroups(r2);
      setSubLayerIds(ids);
      setSelectedRadioGroups(r);
      setSelected(ids.concat(a.map((item) => item.id)));
      setOpen(
        a
          .concat(r)
          .concat(p)
          .map((item) => item.id)
      );
      setDeps({ all: d, parent: dp, child: c });
    });
  }, [layerGroupsDefault, map]);

  // apply selection state to map
  useEffect(() => {
    subLayerIds.forEach((id) => {
      if (map.getLayer(id)) {
        const visible = map.getLayoutProperty(id, "visibility") !== "none";
        if (visible && !isSelected(id)) {
          map.setLayoutProperty(id, "visibility", "none");
        } else if (!visible && isSelected(id)) {
          map.setLayoutProperty(id, "visibility", "visible");
        }
      }
    });
    Object.keys(radioGroups).forEach((group) => {
      radioGroups[group].forEach((id) => {
        if (map.getLayer(id)) {
          const visible = map.getLayoutProperty(id, "visibility") !== "none";
          if (visible && !isSelected(id, group)) {
            map.setLayoutProperty(id, "visibility", "none");
          } else if (!visible && isSelected(id, group)) {
            map.setLayoutProperty(id, "visibility", "visible");
          }
        }
      });
    });
  }, [map, subLayerIds, radioGroups, isSelected]);

  return (
    <>
      <ControlButton isEnabled={layerControlVisible} toggle={toggleLayerControlVisible} />
      {style && (
        <ControlPanel
          parents={parents}
          style={style}
          mapHeight={mapHeight}
          isVisible={layerControlVisible}
          isOpened={isOpened}
          isSelected={isSelected}
          onSelect={onSelect}
          onOpen={onOpen}
        />
      )}
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
