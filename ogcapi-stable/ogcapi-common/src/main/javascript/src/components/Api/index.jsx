import React, { useState, useCallback } from "react";
import PropTypes from "prop-types";
import merge from "deepmerge";
import { useFassets } from "feature-u";

import { Box, Accordion } from "grommet";

import { apiBuildingBlocks } from "../constants";
import Section from "./Section";

const ServiceEditApi = ({
  api,
  defaults,
  isDefaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  // TODO: now only needed for defaults, get merged values from backend
  const mergedBuildingBlocks = {};

  api.forEach((ext) => {
    const bb = ext.buildingBlock;
    if (mergedBuildingBlocks[bb]) {
      mergedBuildingBlocks[bb] = merge(mergedBuildingBlocks[bb], ext);
    } else {
      mergedBuildingBlocks[bb] = ext;
    }
  });

  // TODO: mergedBuildingBlocksDefault, pass to section
  const mergedBuildingBlocksDefault = {};

  if (defaults && defaults.api) {
    defaults.api.forEach((ext) => {
      const bb = ext.buildingBlock;
      if (mergedBuildingBlocksDefault[bb]) {
        mergedBuildingBlocksDefault[bb] = merge(
          mergedBuildingBlocksDefault[bb],
          ext
        );
      } else {
        mergedBuildingBlocksDefault[bb] = ext;
      }
    });
  }
  api.forEach((ext) => {
    const bb = ext.buildingBlock;
    if (!mergedBuildingBlocksDefault[bb]) {
      mergedBuildingBlocksDefault[bb] = {};
    }
  });

  //console.log("API", mergedBuildingBlocks);
  //console.log("DEFAULTS", mergedBuildingBlocksDefault);

  const buildingBlocks = useFassets(apiBuildingBlocks());
  //console.log("BB", buildingBlocks);

  const [activeIndex, setActiveIndex] = useState([]);

  const onBuildingBlockChange = (bbid) =>
    useCallback(
      (change) => onChange({ api: [{ buildingBlock: bbid, ...change }] }),
      []
    );

  return (
    <Box pad={{ horizontal: "small", vertical: "medium" }} fill="horizontal">
      <Accordion
        animate
        activeIndex={activeIndex}
        onActive={(newActiveIndex) => setActiveIndex(newActiveIndex)}
      >
        {buildingBlocks
          .sort((a, b) => a.sortPriority - b.sortPriority)
          .map((bb, i) => (
            <Section
              {...bb}
              key={bb.id}
              isActive={activeIndex.includes(i)}
              isDefaults={isDefaults}
              data={mergedBuildingBlocks[bb.id]}
              defaults={mergedBuildingBlocksDefault[bb.id]}
              inheritedLabel={inheritedLabel}
              debounce={debounce}
              onPending={onPending}
              onChange={onBuildingBlockChange(bb.id)}
            />
          ))}
      </Accordion>
    </Box>
  );
};

ServiceEditApi.displayName = "ServiceEditApi";

ServiceEditApi.propTypes = {
  onChange: PropTypes.func.isRequired,
};

export default ServiceEditApi;
