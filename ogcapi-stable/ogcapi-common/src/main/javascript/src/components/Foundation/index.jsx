import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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

  const { t } = useTranslation();

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
          help={t("building_blocks:FOUNDATION.apiCatalogLabel")}
        />
      )}
      {isDefaults && (
        <TextField
          name="apiCatalogDescription"
          label="API catalog description"
          help={t("building_blocks:FOUNDATION.apiCatalogDescription")}
        />
      )}
      <ToggleField
        name="useLangParameter"
        label="Use 'lang' parameter"
        help={t("building_blocks:FOUNDATION.useLangParameter")}
      />
      <ToggleField
        name="includeLinkHeader"
        label="Include 'link' header"
        help={t("building_blocks:FOUNDATION.includeLinkHeader")}
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
