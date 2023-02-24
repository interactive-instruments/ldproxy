import React from "react";
import PropTypes from "prop-types";

import { Input, FormText } from "reactstrap";

const ValueField = ({ value, saveValue, valueKey, code, integerKeys, enumKeys }) => {
  return (
    <>
      {
        // eslint-disable-next-line no-nested-ternary
        enumKeys.includes(valueKey) ? (
          <Input
            type="select"
            size="sm"
            name="value"
            className="mr-2"
            value={value}
            onChange={saveValue}
          >
            <option value="" className="d-none">
              none
            </option>
            {Object.keys(code[valueKey]).map((item) => (
              <option value={code[valueKey][item]} key={item}>
                {code[valueKey][item]}
              </option>
            ))}
          </Input>
        ) : integerKeys.includes(valueKey) ? (
          <Input
            type="number"
            size="sm"
            name="value"
            placeholder="Enter Number"
            className="mr-2"
            value={value}
            onChange={saveValue}
          />
        ) : (
          <>
            <Input
              type="text"
              size="sm"
              name="value"
              placeholder="filter pattern"
              className="mr-2"
              value={value}
              onChange={saveValue}
            />
            <FormText>Use * as wildcard</FormText>
          </>
        )
      }
    </>
  );
};

export default ValueField;

ValueField.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  code: PropTypes.object.isRequired,
  enumKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
  integerKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
  valueKey: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  saveValue: PropTypes.func.isRequired,
};

ValueField.defaultProps = {};
