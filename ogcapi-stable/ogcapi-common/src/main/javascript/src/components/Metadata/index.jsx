import React, { useCallback } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

import { Box } from "grommet";
import { AutoForm, TextField, getFieldsDefault } from "@xtraplatform/core";

const fieldsTransformation = {
  keywords: {
    from: (value) => (Array.isArray(value) ? value.join() : value),
    to: (value) =>
      typeof value === "string"
        ? value
            .split(",")
            .map((keyword) => keyword.trim())
            .filter((keyword) => keyword.length > 0)
        : value,
  },
};

const Metadata = ({ metadata, defaults, debounce, onPending, onChange }) => {
  const fields = {
    contactName: metadata.contactName,
    contactUrl: metadata.contactUrl,
    contactEmail: metadata.contactEmail,
    contactPhone: metadata.contactPhone,
    licenseName: metadata.licenseName,
    licenseUrl: metadata.licenseUrl,
    keywords: metadata.keywords || [],
    version: metadata.version,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults.metadata);

  const onMetadataChange = useCallback(
    (change) => onChange({ metadata: change }),
    []
  );

  const { t } = useTranslation();

  return (
    <Box pad={{ horizontal: "small", vertical: "medium" }} fill="horizontal">
      <AutoForm
        fields={fields}
        fieldsDefault={fieldsDefault}
        fieldsTransformation={fieldsTransformation}
        inheritedLabel="Service Defaults"
        debounce={debounce}
        onPending={onPending}
        onChange={onMetadataChange}
      >
        <TextField
          name="contactName"
          label={t("services/ogc_api:Metadata.contactName._label")}
          help={t("services/ogc_api:Metadata.contactName._description")}
        />
        <TextField
          name="contactUrl"
          label={t("services/ogc_api:Metadata.contactUrl._label")}
          help={t("services/ogc_api:Metadata.contactUrl._description")}
          type="url"
        />
        <TextField
          name="contactEmail"
          label={t("services/ogc_api:Metadata.contactEmail._label")}
          help={t("services/ogc_api:Metadata.contactEmail._description")}
          type="email"
        />
        <TextField
          name="contactPhone"
          label={t("services/ogc_api:Metadata.contactPhone._label")}
          help={t("services/ogc_api:Metadata.contactPhone._description")}
        />
        <TextField
          name="licenseName"
          label={t("services/ogc_api:Metadata.licenseName._label")}
          help={t("services/ogc_api:Metadata.licenseName._description")}
        />
        <TextField
          name="licenseUrl"
          label={t("services/ogc_api:Metadata.licenseUrl._label")}
          help={t("services/ogc_api:Metadata.licenseUrl._description")}
          type="url"
        />
        <TextField
          area
          name="keywords"
          label={t("services/ogc_api:Metadata.keywords._label")}
          help={t("services/ogc_api:Metadata.keywords._description")}
        />
        <TextField
          name="version"
          label={t("services/ogc_api:Metadata.version._label")}
          help={t("services/ogc_api:Metadata.version._description")}
        />
      </AutoForm>
    </Box>
  );
};

Metadata.displayName = "Metadata";

Metadata.propTypes = {
  metadata: PropTypes.shape({
    keywords: PropTypes.arrayOf(PropTypes.string),
  }),
  defaults: PropTypes.object,
  onChange: PropTypes.func.isRequired,
};

Metadata.defaultProps = {
  metadata: {
    keywords: [],
  },
  defaults: {},
};

export default Metadata;
