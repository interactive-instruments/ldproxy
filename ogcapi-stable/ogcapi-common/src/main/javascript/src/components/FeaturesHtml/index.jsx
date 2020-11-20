import React from "react";
import PropTypes from "prop-types";

import { AutoForm, SelectField, getFieldsDefault } from "@xtraplatform/core";

const FeaturesHtml = ({
  layout,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    layout,
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
      <SelectField
        name="layout"
        label="Layout"
        help="TODO"
        options={["COMPLEX_OBJECTS", "CLASSIC"]}
      />
    </AutoForm>
  );
};

FeaturesHtml.displayName = "FeaturesHtml";

FeaturesHtml.propTypes = {
  onChange: PropTypes.func.isRequired,
};

FeaturesHtml.defaultProps = {};

export default FeaturesHtml;
