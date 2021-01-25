import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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
      <SelectField
        name="layout"
        label={t("building_blocks:FEATURES_HTML.layout._label")}
        help={t("building_blocks:FEATURES_HTML.layout._description")}
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
