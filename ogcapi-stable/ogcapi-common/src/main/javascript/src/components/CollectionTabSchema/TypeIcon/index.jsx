import React from "react";
import PropTypes from "prop-types";

import { Box } from "grommet";
import { Location, Clock, CheckboxSelected } from "grommet-icons";
import { TextIcon } from "@xtraplatform/core";

const TypeIcon = ({ type, valueType }) => {
  const actualType = valueType || type;

  switch (actualType) {
    case "GEOMETRY":
      return (
        <Box title="geometry">
          <Location size="list" />
        </Box>
      );
    case "DATETIME":
      return (
        <Box title="datetime">
          <Clock size="list" />
        </Box>
      );
    case "INTEGER":
      return (
        <Box title="integer">
          <TextIcon text="12" size="list" />
        </Box>
      );
    case "FLOAT":
      return (
        <Box title="float">
          <TextIcon text=".1" size="list" />
        </Box>
      );
    case "STRING":
      return (
        <Box title="string">
          <TextIcon text="ab" size="list" />
        </Box>
      );
    case "BOOLEAN":
      return (
        <Box title="boolean">
          <CheckboxSelected size="list" />
        </Box>
      );
    case "OBJECT":
    case "OBJECT_ARRAY":
      return (
        <Box title="object">
          <TextIcon text="{}" size="list" />
        </Box>
      );
    default:
      return (
        <Box title="unknown">
          <TextIcon text="?" size="list" />
        </Box>
      );
  }
};

TypeIcon.propTypes = {
  type: PropTypes.string,
  valueType: PropTypes.string,
};

TypeIcon.defaultProps = {
  type: null,
  valueType: null,
};

TypeIcon.displayName = "TypeIcon";

export default TypeIcon;
