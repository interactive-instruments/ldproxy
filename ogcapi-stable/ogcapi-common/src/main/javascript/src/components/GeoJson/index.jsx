import React, { useState } from "react";
import PropTypes from "prop-types";

import {
  AutoForm,
  SelectField,
  getFieldsDefault,
  mergedFields,
} from "@xtraplatform/core";

const GeoJson = ({
  nestedObjectStrategy,
  multiplicityStrategy,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    nestedObjectStrategy,
    multiplicityStrategy,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  const [state, setState] = useState(mergedFields(fields, fieldsDefault));

  return (
    <AutoForm
      fields={fields}
      fieldsDefault={fieldsDefault}
      inheritedLabel={inheritedLabel}
      values={state}
      setValues={setState}
      debounce={debounce}
      onPending={onPending}
      onChange={onChange}
    >
      <SelectField
        name="nestedObjectStrategy"
        label="Nested object strategy"
        help="TODO"
        options={["NEST", "FLATTEN"]}
      />
      <SelectField
        name="multiplicityStrategy"
        label="Multiplicity strategy"
        help="TODO"
        options={["ARRAY", "SUFFIX"]}
        value={state.nestedObjectStrategy === "NEST" ? "ARRAY" : "SUFFIX"}
        disabled
      />
    </AutoForm>
  );
};

GeoJson.displayName = "GeoJson";

GeoJson.propTypes = {
  onChange: PropTypes.func.isRequired,
};

GeoJson.defaultProps = {};

export default GeoJson;
