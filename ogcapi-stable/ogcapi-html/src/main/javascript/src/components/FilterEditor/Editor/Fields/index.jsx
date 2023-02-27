import React, { useState } from "react";
import PropTypes from "prop-types";
import "bootstrap/dist/css/bootstrap.min.css";
import { Button, ButtonGroup, Form, FormGroup, Input, Row, Col } from "reactstrap";
import FilterValueField from "./FilterValueField";
import ValueField from "./ValueField";

const FieldFilter = ({
  fields,
  onAdd,
  filters,
  deleteFilters,
  code,
  titleForFilter,
  integerKeys,
  booleanProperty,
}) => {
  const [field, setField] = useState("");
  const [value, setValue] = useState("");
  const [changedValue, setChangedValue] = useState("");

  const selectField = (event) => setField(event.option ? event.option.value : event.target.value);

  const saveValue = (event) => setValue(event.target.value);

  const filtersToMap = Object.keys(filters).filter(
    (key) => filters[key].remove === false && key !== "bbox" && key !== "datetime"
  );
  const enumKeys = Object.keys(code);

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    onAdd(field, value);
    setValue("");
  };

  const overwriteFilters = (item) => () => {
    const updatedFilterValue = { ...changedValue };
    onAdd(item, updatedFilterValue[item].value);
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
            <ValueField
              valueKey={field}
              value={value}
              saveValue={saveValue}
              code={code}
              integerKeys={integerKeys}
              enumKeys={enumKeys}
              booleanProperty={booleanProperty}
            />
          </FormGroup>
        </Col>
        <Col md="2">
          <Button color="primary" size="sm" disabled={field === ""} onClick={save}>
            Add
          </Button>
        </Col>
      </Row>
      <>
        {filtersToMap.map((key) => (
          <Row key={key}>
            <Col md="5">
              <Input
                type="text"
                size="sm"
                name="selectedField"
                id={`input1-${key}`}
                className="mr-2"
                disabled="true"
                defaultValue={titleForFilter[key]}
              />
            </Col>
            <Col md="5">
              <FormGroup>
                <FilterValueField
                  code={code}
                  filterKey={key}
                  filters={filters}
                  setChangedValue={setChangedValue}
                  changedValue={changedValue}
                  enumKeys={enumKeys}
                  integerKeys={integerKeys}
                  booleanProperty={booleanProperty}
                />
              </FormGroup>
            </Col>
            <Col md="2">
              <ButtonGroup>
                <Button
                  color="primary"
                  size="sm"
                  style={{ width: "40px", height: "30px" }}
                  onClick={overwriteFilters(key)}
                >
                  {"\u2713"}
                </Button>
                <Button
                  color="danger"
                  size="sm"
                  style={{ width: "40px", height: "30px" }}
                  onClick={deleteFilters(key)}
                >
                  {"\u2716"}
                </Button>
              </ButtonGroup>
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
  // eslint-disable-next-line react/forbid-prop-types
  filters: PropTypes.object.isRequired,
  deleteFilters: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  code: PropTypes.object.isRequired,
  titleForFilter: PropTypes.objectOf(PropTypes.string).isRequired,
  integerKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
  booleanProperty: PropTypes.arrayOf(PropTypes.string).isRequired,
};

FieldFilter.defaultProps = {};

export default FieldFilter;
