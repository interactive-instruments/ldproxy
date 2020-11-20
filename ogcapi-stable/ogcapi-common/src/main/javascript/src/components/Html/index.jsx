import React from "react";
import PropTypes from "prop-types";

import {
  AutoForm,
  TextField,
  ToggleField,
  getFieldsDefault,
} from "@xtraplatform/core";

const fieldsTransformation = {
  noIndexEnabled: {
    from: (value) => !value,
    to: (value) => !value,
  },
};

const Html = ({
  noIndexEnabled,
  schemaOrgEnabled,
  collectionDescriptionsInOverview,
  footerText,
  legalName,
  legalUrl,
  privacyName,
  privacyUrl,
  leafletUrl,
  leafletAttribution,
  openLayersUrl,
  openLayersAttribution,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    noIndexEnabled,
    schemaOrgEnabled,
    collectionDescriptionsInOverview,
    footerText,
    legalName,
    legalUrl,
    privacyName,
    privacyUrl,
    leafletUrl,
    leafletAttribution,
    openLayersUrl,
    openLayersAttribution,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

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
      <ToggleField
        name="noIndexEnabled"
        label="Enable search engine indexing"
        help="TODO"
      />
      <ToggleField
        name="schemaOrgEnabled"
        label="Enable schema.org annotations"
        help="TODO"
      />
      <ToggleField
        name="collectionDescriptionsInOverview"
        label="Show descriptions in collections overview"
        help="TODO"
      />
      <TextField area name="footerText" label="Footer text" help="TODO" />
      <TextField name="legalName" label="Legal notice label" help="TODO" />
      <TextField
        name="legalUrl"
        label="Legal notice URL"
        help="TODO"
        type="url"
      />
      <TextField name="privacyName" label="Privacy notice label" help="TODO" />
      <TextField
        name="privacyUrl"
        label="Privacy notice URL"
        help="TODO"
        type="url"
      />
      <TextField
        name="leafletUrl"
        label="Leaflet background map URL template"
        help="TODO"
        type="url"
      />
      <TextField
        area
        name="leafletAttribution"
        label="Leaflet source attribution"
        help="TODO"
      />
      <TextField
        name="openLayersUrl"
        label="OpenLayers background map URL template"
        help="TODO"
        type="url"
      />
      <TextField
        area
        name="openLayersAttribution"
        label="OpenLayers source attribution"
        help="TODO"
      />
    </AutoForm>
  );
};

Html.displayName = "Html";

Html.propTypes = {
  onChange: PropTypes.func.isRequired,
};

Html.defaultProps = {};

export default Html;
