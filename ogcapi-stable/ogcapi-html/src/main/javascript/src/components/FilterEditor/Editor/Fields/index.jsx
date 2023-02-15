import React, { useState } from "react";
import PropTypes from "prop-types";
//import "bootstrap/dist/css/bootstrap.min.css";
import ValueSelectField from "./ValueSelectField";

import { Button, Form, FormGroup, Input, FormText, Row, Col } from "reactstrap";

const FieldFilter = ({ fields, onAdd, filters, deleteFilters, code, titleForFilter }) => {
  const [field, setField] = useState("");
  const [value, setValue] = useState("");
  const [changedValue, setChangedValue] = useState("");

  const selectField = (event) => setField(event.option ? event.option.value : event.target.value);

  const saveValue = (event) => setValue(event.target.value);

  let filtersToMap = Object.keys(filters).filter((field) => filters[field].remove === false);
  let enumKeys = Object.keys(code);

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    onAdd(field, value);
    setValue("");
  };

  const overwriteFilters = (field) => () => {
    const updatedFilterValue = { ...changedValue };
    onAdd(field, updatedFilterValue[field].value);
  };
  return (
    <Form onSubmit={save}>
      <p className="text-muted text-uppercase">field</p>
      <Row>
        <Col md="5">
          <FormGroup>
            <Input
              type="select"
              size="sm"
              name="field"
              className={`mr-2${field === "" ? " text-muted" : ""}`}
              value={field}
              onChange={selectField}
            >
              <option value="" className="d-none">
                none
              </option>
              {Object.keys(fields).map((f) => (
                <option value={f} key={f}>
                  {fields[f]}
                </option>
              ))}
            </Input>
          </FormGroup>
        </Col>
        <Col md="5">
          <FormGroup>
            {enumKeys.includes(field) ? (
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
                {Object.keys(code[field]).map((key) => (
                  <option value={code[field][key]} key={key}>
                    {code[field][key]}
                  </option>
                ))}
              </Input>
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
            )}
          </FormGroup>
        </Col>
        <Col md="2">
          <Button color="primary" size="sm" disabled={field === ""} onClick={save}>
            Add
          </Button>
        </Col>
      </Row>
      <>
        {filtersToMap.map((field) => (
          <Row key={field}>
            <Col md="5">
              <Input
                type="text"
                size="sm"
                name="selectedField"
                id={`input1-${field}`}
                className="mr-2"
                disabled={true}
                defaultValue={titleForFilter[field]}
              ></Input>
            </Col>
            <Col md="5">
              <FormGroup>
                {enumKeys.includes(field) ? (
                  <ValueSelectField
                    code={code}
                    field={field}
                    filters={filters}
                    setChangedValue={setChangedValue}
                    changedValue={changedValue}
                  />
                ) : (
                  <Input
                    type="text"
                    size="sm"
                    name="selectedValue"
                    id={`input-${field}`}
                    className="mr-2"
                    placeholder={filters[field].value}
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
                  ></Input>
                )}
              </FormGroup>
            </Col>
            <Col md="2">
              <Button
                color="primary"
                size="sm"
                style={{
                  width: "40px",
                  height: "30px",
                  margin: "1px",
                  fontWeight: "bold",
                }}
                disabled={field === ""}
                onClick={overwriteFilters(field)}
              >
                +
              </Button>
              <Button
                color="danger"
                size="sm"
                style={{ width: "40px", height: "30px", margin: "1px" }}
                disabled={field === ""}
                onClick={deleteFilters(field)}
              >
                x
              </Button>
            </Col>
          </Row>
        ))}
      </>
    </Form>
  );
};

FieldFilter.displayName = "FieldFilter";

FieldFilter.propTypes = {
  fields: PropTypes.objectOf(PropTypes.string),
  onAdd: PropTypes.func,
};

FieldFilter.defaultProps = {};

export default FieldFilter;
