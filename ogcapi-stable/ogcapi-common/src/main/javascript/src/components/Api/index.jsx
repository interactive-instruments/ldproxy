import React, { useState, useCallback } from "react";
import PropTypes from "prop-types";
import merge from "deepmerge";
import { useFassets } from "feature-u";

import { Box, Accordion } from "grommet";

import { apiBuildingBlocks } from "../constants";
import Section from "./Section";

const ServiceEditApi = ({
  id,
  api,
  defaults,
  isDefaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const api2 = api || []; //TODO: why do neither the props nor the deconstruct defaults work?

  // TODO: now only needed for defaults, get merged values from backend
  const mergedBuildingBlocks = {};

  api2.forEach((ext) => {
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
  api2.forEach((ext) => {
    const bb = ext.buildingBlock;
    if (!mergedBuildingBlocksDefault[bb]) {
      mergedBuildingBlocksDefault[bb] = {};
    }
  });

  //console.log("API", mergedBuildingBlocks);
  //console.log("DEFAULTS", mergedBuildingBlocksDefault);

  const buildingBlocks = useFassets(apiBuildingBlocks());
  //console.log("BB", buildingBlocks);

  const dependents = {};
  buildingBlocks.forEach((bb) => {
    const deps = buildingBlocks.filter(
      (bb2) =>
        bb2.dependencies &&
        bb2.dependencies.includes(bb.id.toLowerCase()) &&
        mergedBuildingBlocksDefault[bb2.id].enabled &&
        (!mergedBuildingBlocks[bb2.id] ||
          mergedBuildingBlocks[bb2.id].enabled !== false)
    );
    dependents[bb.id] = deps;
  });
  const dependees = {};
  buildingBlocks.forEach((bb) => {
    const deps = buildingBlocks.filter(
      (bb2) =>
        bb.dependencies &&
        bb.dependencies.includes(bb2.id.toLowerCase()) &&
        (!mergedBuildingBlocksDefault[bb2.id].enabled ||
          (mergedBuildingBlocks[bb2.id] &&
            mergedBuildingBlocks[bb2.id].enabled === false))
    );
    dependees[bb.id] = deps;
  });

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
              key={id + bb.id}
              isActive={activeIndex.includes(i)}
              isDefaults={isDefaults}
              data={mergedBuildingBlocks[bb.id]}
              defaults={mergedBuildingBlocksDefault[bb.id]}
              inheritedLabel={inheritedLabel}
              debounce={debounce}
              dependents={dependents[bb.id]}
              dependees={dependees[bb.id] || []}
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
