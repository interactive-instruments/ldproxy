import React, { useState, useEffect, useCallback } from "react";
import PropTypes from "prop-types";
import ControlPanel from "./ControlPanel";
import ControlButton from "./ControlButton";
import { parse, initialCfg } from "./config";

// eslint-disable-next-line no-unused-vars
const Control = ({ entries, maxHeight, map, opened, onlyLegend, preferStyle }) => {
  const [isVisible, setIsVisible] = useState(opened);
  const [isControlable, setIsControlable] = useState(!onlyLegend);
  const [cfg, setCfg] = useState(initialCfg);
  const [selectedRadioGroups, setSelectedRadioGroups] = useState(initialCfg.radioIds);
  const [selected, setSelected] = useState(initialCfg.allIds);
  const [open, setOpen] = useState(initialCfg.groupIds);

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
      setSelected(selected.filter((id2) => !cfg.deps[id].includes(id2)));
    } else {
      const newSelected = [...selected, id];

      cfg.depsChild[id].forEach((child) => {
        if (!newSelected.includes(child)) {
          newSelected.push(child);
        }
      });

      cfg.depsParent[id].forEach((parent) => {
        if (
          !newSelected.includes(parent) &&
          cfg.depsChild[parent].every((child) => newSelected.includes(child))
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
    setIsVisible(!isVisible);
  };

  // initialize state from configuration when style is loaded
  useEffect(() => {
    map.on("style.load", () => {
      const config = parse(map.getStyle(), entries, preferStyle);

      setCfg(config);
      if (config.opened === true) setIsVisible(true);
      if (config.onlyLegend === true) setIsControlable(false);
      setSelectedRadioGroups(config.radioIds);
      setSelected(config.allIds);
      setOpen(config.groupIds);
    });
  }, [entries, map, preferStyle]);

  // apply selection state to map
  useEffect(() => {
    cfg.layerIds.forEach((id) => {
      if (map.getLayer(id)) {
        const visible = map.getLayoutProperty(id, "visibility") !== "none";
        if (visible && !isSelected(id)) {
          map.setLayoutProperty(id, "visibility", "none");
        } else if (!visible && isSelected(id)) {
          map.setLayoutProperty(id, "visibility", "visible");
        }
      }
    });
    Object.keys(cfg.radioGroups).forEach((group) => {
      cfg.radioGroups[group].forEach((id) => {
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
  }, [map, cfg, isSelected]);

  return (
    <>
      <ControlButton isEnabled={isVisible} toggle={toggleLayerControlVisible} />
      {cfg && cfg.style && (
        <ControlPanel
          entries={cfg.entries}
          style={cfg.style}
          maxHeight={maxHeight}
          isVisible={isVisible}
          isControlable={isControlable}
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
  opened: PropTypes.bool,
  onlyLegend: PropTypes.bool,
  preferStyle: PropTypes.bool,
  entries: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.object, PropTypes.string])),
  maxHeight: PropTypes.number.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  map: PropTypes.object.isRequired,
};

Control.defaultProps = {
  opened: false,
  onlyLegend: false,
  preferStyle: true,
  entries: [],
};

export default Control;
