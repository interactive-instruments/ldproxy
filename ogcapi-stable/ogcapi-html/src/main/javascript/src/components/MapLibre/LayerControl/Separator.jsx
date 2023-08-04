import React from "react";
import PropTypes from "prop-types";
import { Row } from "reactstrap";

const Separator = ({ section, first }) => (
  <Row
    style={{
      paddingTop: section ? "5px" : "2px",
      marginTop: section && !first ? "5px" : null,
      borderTop: section && !first ? "1px solid #ddd" : null,
    }}
  />
);

Separator.displayName = "Separator";

Separator.propTypes = {
  section: PropTypes.bool,
  first: PropTypes.bool,
};

Separator.defaultProps = {
  section: false,
  first: false,
};

export default Separator;
