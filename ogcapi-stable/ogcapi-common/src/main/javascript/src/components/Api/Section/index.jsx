import React, { useState } from "react";
import PropTypes from "prop-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";

import { Box, CheckBox, AccordionPanel } from "grommet";
import { FormAdd, FormSubtract } from "grommet-icons";
import {
  AutoForm,
  InfoLabel,
  getFieldsDefault,
  mergedFields,
} from "@xtraplatform/core";

const StyledBox = styled(Box)`
  color: ${(props) =>
    props.color ? props.theme.global.colors[props.color] : "inherit"};
  ${(props) =>
    props.hoverColor &&
    `    
    &:hover {
        color: ${props.theme.global.colors[props.hoverColor]};
    }
    `};
`;

const ApiSection = ({
  id,
  label,
  component: Form,
  data,
  defaults,
  isActive,
  isDefaults,
  inheritedLabel,
  debounce,
  noDisable,
  dependents,
  dependees,
  onPending,
  onChange,
}) => {
  const fields = {
    enabled: data.enabled,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  const [state, setState] = useState(mergedFields(fields, fieldsDefault));

  const { t } = useTranslation();

  const hasDetails = state.enabled && Form !== null;
  const color = state.enabled ? (isActive ? "brand" : null) : "dark-4";

  const rejectToggle =
    (state.enabled && (noDisable || dependents.length > 0)) ||
    (!state.enabled && dependees.length > 0);
  const rejectReason = noDisable
    ? t("services/ogc_api:api.buildingBlocks.essential")
    : state.enabled
    ? t("services/ogc_api:api.buildingBlocks.requiredBy", {
        buildingBlocks: dependents.map((d) => d.label).join(", "),
      })
    : t("services/ogc_api:api.buildingBlocks.dependsOn", {
        buildingBlocks: dependees.map((d) => d.label).join(", "),
      });
  const toggleTooltip = state.enabled
    ? rejectToggle
      ? t("services/ogc_api:api.buildingBlocks.required", {
          reason: rejectReason,
        })
      : t("services/ogc_api:api.buildingBlocks.disable")
    : rejectToggle
    ? t("services/ogc_api:api.buildingBlocks.unavailable", {
        reason: rejectReason,
      })
    : t("services/ogc_api:api.buildingBlocks.enable");

  console.log(id, dependees, rejectToggle, rejectReason);
  return (
    <AccordionPanel
      onFocus={(e) => {
        e.target.blur();
      }}
      header={
        <StyledBox
          direction="row"
          pad={{ horizontal: "none", vertical: "xsmall" }}
          justify="between"
          align="center"
          style={{ cursor: hasDetails ? "pointer" : "default" }}
          onClick={(event) => {
            if (!hasDetails) event.stopPropagation();
          }}
          color={color}
          hoverColor={hasDetails ? "brand" : null}
        >
          <Box direction="row" pad="none" gap="medium" align="center">
            <Box title={toggleTooltip}>
              <AutoForm
                fields={fields}
                fieldsDefault={fieldsDefault}
                inheritedLabel={inheritedLabel}
                values={state}
                setValues={setState}
                debounce={debounce}
                onPending={onPending}
                onChange={onChange}
              >
                <CheckBox
                  toggle
                  name="enabled"
                  disabled={rejectToggle}
                  onClick={(e) => {
                    e.stopPropagation();
                    e.target.blur();
                  }}
                />
              </AutoForm>
            </Box>
            <InfoLabel
              label={t(`building_blocks:${id}._label`)}
              help={t(`building_blocks:${id}._description`)}
              mono={false}
              iconSize="list"
            />
          </Box>
          {hasDetails && (
            <Box
              title={
                isActive
                  ? t("services/ogc_api:api.buildingBlocks.hideDetails")
                  : t("services/ogc_api:api.buildingBlocks.showDetails")
              }
            >
              {isActive ? (
                <FormSubtract color="brand" />
              ) : (
                <FormAdd color="brand" />
              )}
            </Box>
          )}
        </StyledBox>
      }
    >
      {hasDetails && isActive && (
        <Box
          pad={{ horizontal: "none", vertical: "small" }}
          fill="horizontal"
          border="top"
          background="light-1"
        >
          <Form
            {...data}
            defaults={defaults}
            isDefaults={isDefaults}
            inheritedLabel={inheritedLabel}
            debounce={debounce}
            onPending={onPending}
            onChange={onChange}
          />
        </Box>
      )}
    </AccordionPanel>
  );
};

ApiSection.displayName = "ApiSection";

ApiSection.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  component: PropTypes.elementType,
  data: PropTypes.object,
  defaults: PropTypes.object,
  isActive: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired,
};

ApiSection.defaultProps = {
  component: null,
  data: {},
  defaults: {},
};

export default ApiSection;
