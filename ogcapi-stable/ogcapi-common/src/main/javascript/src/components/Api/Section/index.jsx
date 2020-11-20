import React, { useState } from "react";
import PropTypes from "prop-types";
import styled from "styled-components";

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
  label,
  component: Form,
  data,
  defaults,
  isActive,
  isDefaults,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    enabled: data.enabled,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  const [state, setState] = useState(mergedFields(fields, fieldsDefault));

  const hasDetails = state.enabled && Form !== null;
  const color = state.enabled ? (isActive ? "brand" : null) : "dark-4";

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
            <Box
              title={
                state.enabled
                  ? "Disable building block"
                  : "Enable building block"
              }
            >
              <AutoForm
                fields={fields}
                fieldsDefault={fieldsDefault}
                inheritedLabel={"bla"}
                values={state}
                setValues={setState}
                debounce={debounce}
                onPending={onPending}
                onChange={onChange}
              >
                <CheckBox
                  toggle
                  name="enabled"
                  onClick={(e) => {
                    e.stopPropagation();
                    e.target.blur();
                  }}
                />
              </AutoForm>
            </Box>
            <InfoLabel label={label} help="TODO" mono={false} iconSize="list" />
          </Box>
          {hasDetails && (
            <Box title={isActive ? "Hide details" : "Show details"}>
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
