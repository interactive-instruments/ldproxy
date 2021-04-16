import React from "react";
import PropTypes from "prop-types";

import { Avatar } from "grommet";

const Badge = ({ title, children }) => {
  return (
    <Avatar
      size="20px"
      round="xsmall"
      background="neutral-3"
      color="light-1"
      title={title}
    >
      {children}
    </Avatar>
  );
};

Badge.propTypes = {};

Badge.defaultProps = {};

Badge.displayName = "Badge";

export default Badge;
