import React, { useCallback } from "react";
import PropTypes from "prop-types";

import { Box } from "grommet";
import Section from "./Section";

const transformFrom = (orig, path) => {
  const data = {
    enabled: true,
    queryable: false,
    enabledOverview: true,
    rename: null,
    codelist: null,
    format: null,
    nullify: null,
  };

  if (orig.queryables) {
    Object.keys(orig.queryables).forEach((key) => {
      if (
        Array.isArray(orig.queryables[key]) &&
        orig.queryables[key].includes(path)
      ) {
        data.queryable = true;
      }
    });
  }
  if (orig.transformations) {
    if (orig.transformations[path]) {
      if (orig.transformations[path].remove === "ALWAYS") {
        data.enabled = false;
      }
      if (orig.transformations[path].remove === "OVERVIEW") {
        data.enabledOverview = false;
      }
      if (orig.transformations[path].rename) {
        data.rename = orig.transformations[path].rename;
      }
      if (orig.transformations[path].codelist) {
        data.codelist = orig.transformations[path].codelist;
      }
      if (orig.transformations[path].stringFormat) {
        data.format = orig.transformations[path].stringFormat;
      }
      if (orig.transformations[path].dateFormat) {
        data.format = orig.transformations[path].dateFormat;
      }
      if (orig.transformations[path].nullify) {
        data.nullify = orig.transformations[path].nullify.join(",");
      }
    }
  }

  return data;
};

const transformTo = (changes, path, schema) => {
  const data = {};

  const isDate = schema && schema.type === "DATETIME";
  const isGeometry = schema && schema.type === "GEOMETRY";

  if (changes.queryable) {
    const type = isGeometry ? "spatial" : isDate ? "temporal" : "other";
    data.queryables = { [type]: [path] };
  }

  if (changes.enabledOverview === false) {
    if (!data.transformations) data.transformations = {};
    if (!data.transformations[path]) data.transformations[path] = {};
    data.transformations[path].remove = "OVERVIEW";
  }
  if (changes.enabled === false) {
    if (!data.transformations) data.transformations = {};
    if (!data.transformations[path]) data.transformations[path] = {};
    data.transformations[path].remove = "ALWAYS";
  }

  if (changes.rename) {
    if (!data.transformations) data.transformations = {};
    if (!data.transformations[path]) data.transformations[path] = {};
    data.transformations[path].rename = changes.rename;
  }

  if (changes.codelist) {
    if (!data.transformations) data.transformations = {};
    if (!data.transformations[path]) data.transformations[path] = {};
    data.transformations[path].codelist = changes.codelist;
  }

  if (changes.format) {
    if (!data.transformations) data.transformations = {};
    if (!data.transformations[path]) data.transformations[path] = {};
    if (isDate) {
      data.transformations[path].dateFormat = changes.format;
    } else {
      data.transformations[path].stringFormat = changes.format;
    }
  }

  if (changes.nullify) {
    if (!data.transformations) data.transformations = {};
    if (!data.transformations[path]) data.transformations[path] = {};
    data.transformations[path].nullify = changes.nullify.split(",");
  }

  return data;
};

//TODO: props from FeatureSchema
//TODO: extension point for building blocks
const CollectionDataEdit = ({
  id,
  label,
  api,
  schema,
  codelists,
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

  const buildingBlocks = [
    {
      id: "FEATURES_CORE",
      label: "Features Core",
      sortPriority: 60,
      component: Box,
    },
    {
      id: "GEO_JSON",
      label: "Features GeoJSON",
      sortPriority: 70,
      component: Box,
    },
    {
      id: "FEATURES_HTML",
      label: "Features HTML",
      sortPriority: 80,
      component: Box,
    },
  ];

  const disabled =
    mergedBuildingBlocks["FEATURES_CORE"] &&
    mergedBuildingBlocks["FEATURES_CORE"].transformations &&
    mergedBuildingBlocks["FEATURES_CORE"].transformations[id] &&
    mergedBuildingBlocks["FEATURES_CORE"].transformations[id].remove ===
      "ALWAYS";

  const onBuildingBlockChange = (bbid) =>
    useCallback(
      (change) =>
        onChange({
          api: [{ buildingBlock: bbid, ...transformTo(change, id, schema) }],
        }),
      [id, schema, onChange]
    );

  return (
    <Box pad="small">
      {buildingBlocks
        .sort((a, b) => a.sortPriority - b.sortPriority)
        .map((bb, i) => {
          const orig = mergedBuildingBlocks[bb.id] || {};
          const data = transformFrom(orig, id);

          return (
            <Section
              {...bb}
              key={`${id}_${bb.id}`}
              disabled={disabled}
              path={id}
              name={
                id && id.indexOf(".") ? id.substr(id.lastIndexOf(".") + 1) : id
              }
              data={data}
              schema={schema}
              codelists={codelists}
              debounce={debounce}
              onPending={onPending}
              onChange={onBuildingBlockChange(bb.id)}
            />
          );
        })}
    </Box>
  );
};

CollectionDataEdit.propTypes = { api: PropTypes.array };

CollectionDataEdit.defaultProps = { api: [] };

CollectionDataEdit.displayName = "CollectionDataEdit";

export default CollectionDataEdit;
