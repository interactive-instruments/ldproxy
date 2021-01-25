import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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
      <ToggleField
        name="resourcesEnabled"
        label={t("building_blocks:STYLES.resourcesEnabled._label")}
        help={t("building_blocks:STYLES.resourcesEnabled._description")}
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
