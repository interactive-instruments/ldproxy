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
          label={t("services/ogc_api:collections.id._label")}
          help={t("services/ogc_api:collections.id._description")}
          value={id}
          readOnly
        />
        <TextField
          name="label"
          label={t("services/ogc_api:label._label")}
          help={t("services/ogc_api:label._description")}
        />
        <TextField
          area
          name="description"
          label={t("services/ogc_api:description._label")}
          help={t("services/ogc_api:description._description")}
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
