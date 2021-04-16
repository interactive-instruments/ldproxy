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
          label={t("building_blocks:FOUNDATION.apiCatalogLabel._label")}
          help={t("building_blocks:FOUNDATION.apiCatalogLabel._description")}
        />
      )}
      {isDefaults && (
        <TextField
          name="apiCatalogDescription"
          label={t("building_blocks:FOUNDATION.apiCatalogDescription._label")}
          help={t(
            "building_blocks:FOUNDATION.apiCatalogDescription._description"
          )}
        />
      )}
      <ToggleField
        name="useLangParameter"
        label={t("building_blocks:FOUNDATION.useLangParameter._label")}
        help={t("building_blocks:FOUNDATION.useLangParameter._description")}
      />
      <ToggleField
        name="includeLinkHeader"
        label={t("building_blocks:FOUNDATION.includeLinkHeader._label")}
        help={t("building_blocks:FOUNDATION.includeLinkHeader._description")}
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
