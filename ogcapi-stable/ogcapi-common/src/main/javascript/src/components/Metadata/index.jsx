import React, { useCallback } from "react";
import PropTypes from "prop-types";

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
        <TextField name="contactName" label="Contact Name" help="TODO" />
        <TextField
          name="contactUrl"
          label="Contact URL"
          help="TODO"
          type="url"
        />
        <TextField
          name="contactEmail"
          label="Contact Email"
          help="TODO"
          type="email"
        />
        <TextField name="contactPhone" label="Contact Phone" help="TODO" />
        <TextField name="licenseName" label="License Name" help="TODO" />
        <TextField
          name="licenseUrl"
          label="License URL"
          help="TODO"
          type="url"
        />
        <TextField area name="keywords" label="Keywords" help="TODO" />
        <TextField name="version" label="Version" help="TODO" />
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
