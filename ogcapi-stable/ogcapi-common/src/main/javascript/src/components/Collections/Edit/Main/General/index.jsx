import React from "react";
import PropTypes from "prop-types";

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

  return (
    <Box pad={{ horizontal: "small", vertical: "medium" }} fill="horizontal">
      <AutoForm
        fields={fields}
        debounce={debounce}
        onPending={onPending}
        onChange={onChange}
      >
        <TextField name="id" label="Id" help="TODO" value={id} readOnly />
        <TextField name="label" label="Label" help="TODO" />
        <TextField area name="description" label="Description" help="TODO" />
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
