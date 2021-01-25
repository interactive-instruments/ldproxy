import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

import { Box } from "grommet";
import { AutoForm, TextField } from "@xtraplatform/core";

const CollectionEditGeneral = ({
  id,
  label,
  description,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    label,
    description,
  };

  const { t } = useTranslation();

  return (
    <Box pad={{ horizontal: "small", vertical: "medium" }} fill="horizontal">
      <AutoForm
        fields={fields}
        debounce={debounce}
        onPending={onPending}
        onChange={onChange}
      >
        <TextField
          name="id"
          label="Id"
          help={t("services/ogc_api:id")}
          value={id}
          readOnly
        />
        <TextField
          name="label"
          label="Label"
          help={t("services/ogc_api:label")}
        />
        <TextField
          area
          name="description"
          label="Description"
          help={t("services/ogc_api:description")}
        />
      </AutoForm>
    </Box>
  );
};

CollectionEditGeneral.displayName = "CollectionEditGeneral";

CollectionEditGeneral.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string,
  description: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default CollectionEditGeneral;
