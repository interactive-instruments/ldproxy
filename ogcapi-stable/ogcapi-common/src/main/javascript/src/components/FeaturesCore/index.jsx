import React from "react";
import PropTypes from "prop-types";

import { AutoForm, TextField, getFieldsDefault } from "@xtraplatform/core";

const FeaturesCore = ({
  defaultPageSize,
  minimumPageSize,
  maximumPageSize,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    defaultPageSize,
    minimumPageSize,
    maximumPageSize,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  return (
    <AutoForm
      fields={fields}
      fieldsDefault={fieldsDefault}
      inheritedLabel={inheritedLabel}
      debounce={debounce}
      onPending={onPending}
      onChange={onChange}
    >
      <TextField
        name="defaultPageSize"
        label="Default page size"
        help="TODO"
        type="number"
        min="0"
      />
      <TextField
        name="minimumPageSize"
        label="Minimum page size"
        help="TODO"
        type="number"
        min="0"
      />
      <TextField
        name="maximumPageSize"
        label="Maximum page size"
        help="TODO"
        type="number"
        min="0"
      />
    </AutoForm>
  );
};

FeaturesCore.displayName = "FeaturesCore";

FeaturesCore.propTypes = {
  onChange: PropTypes.func.isRequired,
};

FeaturesCore.defaultProps = {};

export default FeaturesCore;
