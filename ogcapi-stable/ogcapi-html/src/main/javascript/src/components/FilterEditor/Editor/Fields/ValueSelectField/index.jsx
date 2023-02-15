import React, { useState } from "react";
import 'bootstrap/dist/css/bootstrap.min.css';

import {
  Button,
  Form,
  FormGroup,
  Input,
  FormText,
  Row,
  Col,
} from 'reactstrap';

const ValueSelectField = ({code, field, filters, changedValue, setChangedValue}) => {
  return(
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
          value: e.target.value
        }
      })
    }
    >
    {Object.keys(code[field]).map((key) => (
      <option value={code[field][key]} key={key}>
        {code[field][key]}
      </option>
    
    ))}
    </Input>
  )
}

export default ValueSelectField;
