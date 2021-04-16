import React, { useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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

  const { t } = useTranslation();

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
        label={t("building_blocks:GEO_JSON.nestedObjectStrategy._label")}
        help={t("building_blocks:GEO_JSON.nestedObjectStrategy._description")}
        options={["NEST", "FLATTEN"]}
      />
      <SelectField
        name="multiplicityStrategy"
        label={t("building_blocks:GEO_JSON.multiplicityStrategy._label")}
        help={t("building_blocks:GEO_JSON.multiplicityStrategy._description")}
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
