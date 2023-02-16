import React, { useState } from "react";
import PropTypes from "prop-types";
import "bootstrap/dist/css/bootstrap.min.css";
import { Button, Form, FormGroup, Input, FormText, Row, Col } from "reactstrap";
import ValueSelectField from "./ValueSelectField";

const FieldFilter = ({ fields, onAdd, filters, deleteFilters, code, titleForFilter }) => {
  const [field, setField] = useState("");
  const [value, setValue] = useState("");
  const [changedValue, setChangedValue] = useState("");

  const selectField = (event) => setField(event.option ? event.option.value : event.target.value);

  const saveValue = (event) => setValue(event.target.value);

  const filtersToMap = Object.keys(filters).filter((key) => filters[key].remove === false);
  const enumKeys = Object.keys(code);

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    onAdd(field, value);
    setValue("");
  };

  const overwriteFilters = (key) => () => {
    const updatedFilterValue = { ...changedValue };
    onAdd(field, updatedFilterValue[key].value);
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
        {filtersToMap.map((item) => (
          <Row key={item}>
            <Col md="5">
              <Input
                type="text"
                size="sm"
                name="selectedField"
                id={`input1-${item}`}
                className="mr-2"
                disabled="true"
                defaultValue={titleForFilter[item]}
              />
            </Col>
            <Col md="5">
              <FormGroup>
                {enumKeys.includes(item) ? (
                  <ValueSelectField
                    code={code}
                    field={item}
                    filters={filters}
                    setChangedValue={setChangedValue}
                    changedValue={changedValue}
                  />
                ) : (
                  <Input
                    type="text"
                    size="sm"
                    name="selectedValue"
                    id={`input-${item}`}
                    className="mr-2"
                    placeholder={filters[item].value}
                    defaultValue={filters[item].value}
                    onChange={(e) =>
                      setChangedValue({
                        ...changedValue,
                        [item]: {
                          item,
                          value: e.target.value,
                        },
                      })
                    }
                  />
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
  fields: PropTypes.objectOf(PropTypes.string).isRequired,
  onAdd: PropTypes.func.isRequired,
  filters: PropTypes.objectOf(PropTypes.oneOfType([PropTypes.number, PropTypes.string])).isRequired,
  deleteFilters: PropTypes.func.isRequired,
  code: PropTypes.objectOf(PropTypes.string).isRequired,
  titleForFilter: PropTypes.objectOf(PropTypes.string).isRequired,
};

FieldFilter.defaultProps = {};

export default FieldFilter;
