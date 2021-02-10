import React, { useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

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

  const { t } = useTranslation();

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
        <InfoLabel
          label={t(`services/ogc_api:collections.data.${id}._label`)}
          help={t(`services/ogc_api:collections.data.${id}._description`)}
          mono={false}
          iconSize="list"
        />
      </Box>
      <ToggleField
        name="enabled"
        label={t("services/ogc_api:collections.data.enabled._label")}
        help={t("services/ogc_api:collections.data.enabled._description")}
        disabled={isDisabled}
      />
      {!isDisabled && isCore && (
        <ToggleField
          name="queryable"
          label={t("services/ogc_api:collections.data.queryable._label")}
          help={t("services/ogc_api:collections.data.queryable._description")}
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (
        <ToggleField
          name="enabledOverview"
          label={t("services/ogc_api:collections.data.enabledOverview._label")}
          help={t(
            "services/ogc_api:collections.data.enabledOverview._description"
          )}
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (
        <TextField
          name="rename"
          label={t("services/ogc_api:collections.data.rename._label")}
          help={t("services/ogc_api:collections.data.rename._description")}
          placeholder={name}
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (isDate || isString) && (
        <TextField
          name="format"
          label={t(
            `services/ogc_api:collections.data.format.${
              isDate ? "date" : "string"
            }._label`
          )}
          help={t(
            `services/ogc_api:collections.data.format.${
              isDate ? "date" : "string"
            }._description`,
            { skipInterpolation: true }
          )}
          disabled={!state.enabled}
        />
      )}
      {!isDisabled && (isNumber || isString) && (
        <SelectField
          name="codelist"
          label={t("services/ogc_api:collections.data.codelist._label")}
          help={t("services/ogc_api:collections.data.codelist._description")}
          disabled={!state.enabled}
          options={options}
          clear={{
            position: "top",
            label: t("services/ogc_api:collections.data.codelist.none._label"),
          }}
          placeholder={t(
            "services/ogc_api:collections.data.codelist.none._label"
          )}
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
          label={t("services/ogc_api:collections.data.nullify._label")}
          help={t("services/ogc_api:collections.data.nullify._description")}
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
