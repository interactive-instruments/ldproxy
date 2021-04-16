import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

import { AutoForm, SelectField, getFieldsDefault } from "@xtraplatform/core";

const fieldsTransformation = {
  additionalCrs: {
    from: (value) =>
      Array.isArray(value)
        ? [...new Set(value.map((crs) => "EPSG:" + crs.code))]
        : value,
    to: (value) =>
      Array.isArray(value)
        ? value.map((crs) => ({
            code: parseInt(crs.substr(crs.indexOf(":") + 1)),
            forceAxisOrder: "NONE",
          }))
        : value,
  },
};

const Crs = ({
  additionalCrs,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    additionalCrs,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  const { t } = useTranslation();

  return (
    <AutoForm
      fields={fields}
      fieldsDefault={fieldsDefault}
      fieldsTransformation={fieldsTransformation}
      inheritedLabel={inheritedLabel}
      debounce={debounce}
      onPending={onPending}
      onChange={onChange}
    >
      <SelectField
        name="additionalCrs"
        label={t("building_blocks:CRS.additionalCrs._label")}
        help={t("building_blocks:CRS.additionalCrs._description")}
        multiple
        placeholder="Select"
        options={[
          "EPSG:4326",
          "EPSG:3857",
          "EPSG:4258",
          "EPSG:3395",
          "EPSG:3034",
          "EPSG:3035",
          "EPSG:25831",
          "EPSG:25832",
          "EPSG:25833",
          "EPSG:25834",
        ]}
      />
    </AutoForm>
  );
};

Crs.displayName = "Crs";

Crs.propTypes = {
  onChange: PropTypes.func.isRequired,
};

Crs.defaultProps = {};

export default Crs;
