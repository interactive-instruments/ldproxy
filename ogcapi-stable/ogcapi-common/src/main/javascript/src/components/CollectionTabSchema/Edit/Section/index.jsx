import React, { useState } from "react";
import PropTypes from "prop-types";

import { Box } from "grommet";
import {
  AutoForm,
  TextField,
  ToggleField,
  SelectField,
  InfoLabel,
  getFieldsDefault,
  mergedFields,
  isFloat,
  isInt,
  bounds,
  lessThan,
  greaterThan,
} from "@xtraplatform/core";

const fieldsTransformation = {
  transformations: {},
};

const DataEditSection = ({
  id,
  label,
  path,
  name,
  data: {
    enabled,
    queryable,
    enabledOverview,
    rename,
    codelist,
    format,
    nullify,
  },
  defaults,
  schema,
  codelists,
  debounce,
  disabled,
  onPending,
  onChange,
}) => {
  const fields = {
    enabled,
    queryable,
    enabledOverview,
    rename,
    codelist,
    format,
    nullify,
  };
  const fieldsDefault = {}; //getFieldsDefault(fields, defaults);

  const [state, setState] = useState(mergedFields(fields, fieldsDefault));
  //console.log("DATA", path, state);

  const isCore = id === "FEATURES_CORE";
  const isDisabled = !isCore && disabled;
  const isDate = schema && schema.type === "DATETIME";
  const isString = schema && schema.type === "STRING";
  const isNumber =
    schema && (schema.type === "INTEGER" || schema.type === "FLOAT");
  const isGeometry = schema && schema.type === "GEOMETRY";
  //console.log("DIS", isDisabled, schema);
  const cl =
    codelists && Array.isArray(codelists) && codelists.map((c) => c.id);
  const [options, setOptions] = useState(cl);

  return (
    <AutoForm
      fields={fields}
      fieldsDefault={fieldsDefault}
      values={state}
      setValues={setState}
      debounce={debounce}
      onPending={onPending}
      onChange={onChange}
    >
      <Box pad="small" margin={{ bottom: "small" }} border="bottom">
        <InfoLabel label={label} help="TODO" mono={false} iconSize="list" />
      </Box>
      <ToggleField
        name="enabled"
        label="Enabled"
        help="TODO"
        disabled={isDisabled}
      />
      {!isDisabled && isCore && (
        <ToggleField
          name="queryable"
          label="Queryable"
          help="TODO"
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (
        <ToggleField
          name="enabledOverview"
          label="Show in collections"
          help="TODO"
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (
        <TextField
          name="rename"
          label="Rename"
          help="TODO"
          placeholder={name}
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (isDate || isString) && (
        <TextField
          name="format"
          label={`${isDate ? "Date" : "String"} format`}
          help="TODO"
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (isNumber || isString) && (
        <SelectField
          name="codelist"
          label="Codelist"
          help="TODO"
          disabled={!state.enabled}
          options={options}
          placeholder="None"
          onClose={() => setOptions(cl)}
          onSearch={(text) => {
            // The line below escapes regular expression special characters:
            // [ \ ^ $ . | ? * + ( )
            const escapedText = text.replace(/[-\\^$*+?.()|[\]{}]/g, "\\$&");

            // Create the regular expression with modified value which
            // handles escaping special characters. Without escaping special
            // characters, errors will appear in the console
            const exp = new RegExp(escapedText, "i");
            setOptions(cl.filter((o) => exp.test(o)));
          }}
        />
      )}
      {!isDisabled && !isGeometry && (
        <TextField
          name="nullify"
          label="Nullify"
          help="TODO"
          disabled={!state.enabled}
        />
      )}
    </AutoForm>
  );
};

DataEditSection.propTypes = {};

DataEditSection.defaultProps = {};

DataEditSection.displayName = "DataEditSection";

export default DataEditSection;
