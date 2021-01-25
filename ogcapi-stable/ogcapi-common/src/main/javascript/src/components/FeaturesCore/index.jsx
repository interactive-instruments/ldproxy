import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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
      <TextField
        name="defaultPageSize"
        label={t("building_blocks:FEATURES_CORE.defaultPageSize._label")}
        help={t("building_blocks:FEATURES_CORE.defaultPageSize._description")}
        type="number"
        min="0"
      />
      <TextField
        name="minimumPageSize"
        label={t("building_blocks:FEATURES_CORE.minimumPageSize._label")}
        help={t("building_blocks:FEATURES_CORE.minimumPageSize._description")}
        type="number"
        min="0"
      />
      <TextField
        name="maximumPageSize"
        label={t("building_blocks:FEATURES_CORE.maximumPageSize._label")}
        help={t("building_blocks:FEATURES_CORE.maximumPageSize._description")}
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
