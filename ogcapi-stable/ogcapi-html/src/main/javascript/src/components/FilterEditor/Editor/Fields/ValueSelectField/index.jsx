import React from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import PropTypes from "prop-types";

import { Input } from "reactstrap";

const ValueSelectField = ({ code, field, filters, changedValue, setChangedValue }) => {
  return (
    <Input
      type="select"
      size="sm"
      name="value"
      className="mr-2"
      defaultValue={filters[field].value}
      onChange={(e) =>
        setChangedValue({
          ...changedValue,
          [field]: {
            field,
            value: e.target.value,
          },
        })
      }
    >
      {Object.keys(code[field]).map((key) => (
        <option value={code[field][key]} key={key}>
          {code[field][key]}
        </option>
      ))}
    </Input>
  );
};

export default ValueSelectField;

ValueSelectField.propTypes = {
  field: PropTypes.string.isRequired,
  filters: PropTypes.object.isRequired,
  code: PropTypes.object.isRequired,
  changedValue: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  setChangedValue: PropTypes.func.isRequired,
};

ValueSelectField.defaultProps = {};
