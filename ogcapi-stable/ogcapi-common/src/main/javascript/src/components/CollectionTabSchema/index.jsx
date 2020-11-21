import React, { useEffect, useState } from "react";
import PropTypes from "prop-types";

import { Box, Avatar } from "grommet";
import { Filter, FingerPrint } from "grommet-icons";
import { Async, TreeList, TextIcon } from "@xtraplatform/core";
import { useProvider, useCodelists } from "@xtraplatform/services";

import TypeIcon from "./TypeIcon";
import Badge from "./Badge";
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
      icon: (
        <TypeIcon
          type={schema.type}
          valueType={schema.valueType}
          role={schema.role}
        />
      ),
      badge: (
        <Box
          direction="row"
          align="center"
          gap="xsmall"
          pad={{ left: "xsmall" }}
        >
          {schema.role === "ID" ? (
            <Badge title="id">
              <FingerPrint size="small" />
            </Badge>
          ) : null}
          {schema.type === "OBJECT_ARRAY" || schema.type === "VALUE_ARRAY" ? (
            <Badge title="array">
              <TextIcon text="[]" size="list" />
            </Badge>
          ) : null}
          {isQueryable ? (
            <Badge title="queryable">
              <Filter size="small" />
            </Badge>
          ) : null}
        </Box>
      ),
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

  const { loading, error, data } = useProvider(providerId);
  const { loading: loading2, error: error2, data: data2 } = useCodelists();
  const [selected, setSelected] = useState(featureType);

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

  const selectedInitial = tree.length > 1 ? tree[1].id : featureType;

  useEffect(() => setSelected(selectedInitial), [setSelected, selectedInitial]);

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
            noExpanders
            tree={tree}
            selected={selectedInitial}
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
