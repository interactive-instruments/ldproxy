import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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
      <ToggleField
        name="noIndexEnabled"
        label={t(`building_blocks:HTML.noIndexEnabled._label`)}
        help={t(`building_blocks:HTML.noIndexEnabled._description`)}
      />
      <ToggleField
        name="schemaOrgEnabled"
        label={t(`building_blocks:HTML.schemaOrgEnabled._label`)}
        help={t(`building_blocks:HTML.schemaOrgEnabled._description`)}
      />
      <ToggleField
        name="collectionDescriptionsInOverview"
        label={t(
          `building_blocks:HTML.collectionDescriptionsInOverview._label`
        )}
        help={t(
          `building_blocks:HTML.collectionDescriptionsInOverview._description`
        )}
      />
      <TextField
        area
        name="footerText"
        label={t(`building_blocks:HTML.footerText._label`)}
        help={t(`building_blocks:HTML.footerText._description`)}
      />
      <TextField
        name="legalName"
        label={t(`building_blocks:HTML.legalName._label`)}
        help={t(`building_blocks:HTML.legalName._description`)}
      />
      <TextField
        name="legalUrl"
        label={t(`building_blocks:HTML.legalUrl._label`)}
        help={t(`building_blocks:HTML.legalUrl._description`)}
        type="url"
      />
      <TextField
        name="privacyName"
        label={t(`building_blocks:HTML.privacyName._label`)}
        help={t(`building_blocks:HTML.privacyName._description`)}
      />
      <TextField
        name="privacyUrl"
        label={t(`building_blocks:HTML.privacyUrl._label`)}
        help={t(`building_blocks:HTML.privacyUrl._description`)}
        type="url"
      />
      <TextField
        name="leafletUrl"
        label={t(`building_blocks:HTML.leafletUrl._label`)}
        help={t(`building_blocks:HTML.leafletUrl._description`)}
        type="url"
      />
      <TextField
        area
        name="leafletAttribution"
        label={t(`building_blocks:HTML.leafletAttribution._label`)}
        help={t(`building_blocks:HTML.leafletAttribution._description`)}
      />
      <TextField
        name="openLayersUrl"
        label={t(`building_blocks:HTML.openLayersUrl._label`)}
        help={t(`building_blocks:HTML.openLayersUrl._description`)}
        type="url"
      />
      <TextField
        area
        name="openLayersAttribution"
        label={t(`building_blocks:HTML.openLayersAttribution._label`)}
        help={t(`building_blocks:HTML.openLayersAttribution._description`)}
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
