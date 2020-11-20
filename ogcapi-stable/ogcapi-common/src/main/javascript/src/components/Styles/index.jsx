import React from "react";
import PropTypes from "prop-types";

import { AutoForm, ToggleField, getFieldsDefault } from "@xtraplatform/core";

const Styles = ({
  resourcesEnabled,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    resourcesEnabled,
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
      <ToggleField
        name="resourcesEnabled"
        label="Resources enabled"
        help="TODO"
      />
    </AutoForm>
  );
};

Styles.displayName = "Styles";

Styles.propTypes = {
  onChange: PropTypes.func.isRequired,
};

Styles.defaultProps = {};

export default Styles;
