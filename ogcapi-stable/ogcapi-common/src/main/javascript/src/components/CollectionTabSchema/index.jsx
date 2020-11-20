import React, { useState } from "react";
import PropTypes from "prop-types";

import { Box, Avatar } from "grommet";
import { Filter } from "grommet-icons";
import {
  Async,
  TreeList,
  InfoLabel,
  useDebounceFields,
} from "@xtraplatform/core";
import { useProvider, useCodelists } from "@xtraplatform/services";

import TypeIcon from "./TypeIcon";
import Edit from "./Edit";

const schemaToTree = (schema, id, label, queryables, parent = null) => {
  const tree = [];

  if (schema) {
    const isQueryable =
      queryables &&
      ((Array.isArray(queryables.other) && queryables.other.includes(id)) ||
        (Array.isArray(queryables.spatial) &&
          queryables.spatial.includes(id)) ||
        (Array.isArray(queryables.temporal) &&
          queryables.temporal.includes(id)));
    tree.push({
      id: id,
      label: label,
      parent: parent,
      icon: <TypeIcon type={schema.type} role={schema.role} />,
      badge: isQueryable ? (
        <Avatar size="small" background="accent-3" title="queryable">
          <Filter size="small" />
        </Avatar>
      ) : null,
    });

    Object.keys(schema.properties).forEach((childId) =>
      tree.push(
        ...schemaToTree(
          schema.properties[childId],
          parent ? `${id}.${childId}` : childId,
          childId,
          queryables,
          id
        )
      )
    );
  }

  return tree;
};

const getValue = (schema, path) => {
  if (!path && path.indexOf(".") === -1) {
    return null;
  }
  let current = schema || {};
  path
    .split(".")
    .forEach(
      (elem) =>
        (current =
          current.properties && current.properties[elem]
            ? current.properties[elem]
            : {})
    );
  return current;
};

const isValue = (schema, path) => {
  const current = getValue(schema, path);
  return (
    current &&
    ["INTEGER", "FLOAT", "STRING", "BOOLEAN", "DATETIME", "GEOMETRY"].includes(
      current.type
    )
  );
};

const CollectionTabSchema = ({
  serviceId,
  id,
  label,
  description,
  api,
  debounce,
  onPending,
  onChange,
}) => {
  //TODO: api does not contain service buildingBlocks
  const featuresCore = api.find((bb) => bb.buildingBlock === "FEATURES_CORE");
  const providerId =
    (featuresCore && featuresCore.featureProvider) || serviceId;
  const featureType = (featuresCore && featuresCore.featureType) || id;

  const [selected, setSelected] = useState(featureType);
  const { loading, error, data } = useProvider(providerId);
  const { loading: loading2, error: error2, data: data2 } = useCodelists();

  const schema =
    data &&
    data.provider &&
    data.provider.types &&
    data.provider.types[featureType];
  const codelists = data2 && data2.codelists;

  const tree = schemaToTree(
    schema,
    featureType,
    featureType,
    featuresCore.queryables
  );

  //TODO: selected has to be full path to property
  return (
    <Async loading={loading || loading2} error={error || error2}>
      <Box direction="row" fill>
        <Box
          pad={{ horizontal: "small", vertical: "medium" }}
          fill="vertical"
          basis="1/2"
          overflow={{ vertical: "auto" }}
        >
          <TreeList
            tree={tree}
            expanded={[featureType]}
            selected={featureType}
            hideRootExpander
            onSelect={setSelected}
          />
        </Box>
        <Box
          fill="vertical"
          basis="1/2"
          background="light-1"
          overflow={{ vertical: "auto" }}
        >
          {isValue(schema, selected) && (
            <Edit
              id={selected}
              schema={getValue(schema, selected)}
              api={api}
              codelists={codelists}
              debounce={debounce}
              onPending={onPending}
              onChange={onChange}
            />
          )}
        </Box>
      </Box>
    </Async>
  );
};

CollectionTabSchema.displayName = "CollectionTabSchema";

CollectionTabSchema.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string,
  description: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default CollectionTabSchema;
