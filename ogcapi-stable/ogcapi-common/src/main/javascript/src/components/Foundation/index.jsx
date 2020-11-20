import React from "react";
import PropTypes from "prop-types";

import {
  AutoForm,
  TextField,
  ToggleField,
  getFieldsDefault,
} from "@xtraplatform/core";

const Foundation = ({
  apiCatalogLabel,
  apiCatalogDescription,
  includeLinkHeader,
  useLangParameter,
  inheritedLabel,
  defaults,
  isDefaults,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    apiCatalogLabel,
    apiCatalogDescription,
    includeLinkHeader,
    useLangParameter,
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
      {isDefaults && (
        <TextField
          name="apiCatalogLabel"
          label="API catalog label"
          help="TODO"
        />
      )}
      {isDefaults && (
        <TextField
          name="apiCatalogDescription"
          label="API catalog description"
          help="TODO"
        />
      )}
      <ToggleField
        name="useLangParameter"
        label="Use 'lang' parameter"
        help="TODO"
      />
      <ToggleField
        name="includeLinkHeader"
        label="Include 'link' header"
        help="TODO"
      />
    </AutoForm>
  );
};

Foundation.displayName = "Foundation";

Foundation.propTypes = {
  onChange: PropTypes.func.isRequired,
};

Foundation.defaultProps = {};

export default Foundation;
