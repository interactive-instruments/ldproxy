import React from "react";
import PropTypes from "prop-types";

import { Box } from "grommet";
import {
  Location,
  Clock,
  Table,
  FingerPrint,
  Down,
  CheckboxSelected,
  BlockQuote,
  BarChart,
} from "grommet-icons";

const TypeIcon = ({ type, role }) => {
  switch (role) {
    case "ID":
      return (
        <Box title="id">
          <FingerPrint size="list" />
        </Box>
      );
    default:
      break;
  }

  switch (type) {
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
          <BarChart size="list" />
        </Box>
      );
    case "FLOAT":
      return (
        <Box title="float">
          <BarChart size="list" />
        </Box>
      );
    case "STRING":
      return (
        <Box title="string">
          <BlockQuote size="list" />
        </Box>
      );
    case "BOOLEAN":
      return (
        <Box title="boolean">
          <CheckboxSelected size="list" />
        </Box>
      );
    case "OBJECT":
      return (
        <Box title="type">
          <Down size="list" />
        </Box>
      );
    default:
      break;
  }

  return null;
};

TypeIcon.propTypes = {
  type: PropTypes.string,
  role: PropTypes.string,
};

TypeIcon.defaultProps = {
  type: null,
  role: null,
};

TypeIcon.displayName = "TypeIcon";

export default TypeIcon;
