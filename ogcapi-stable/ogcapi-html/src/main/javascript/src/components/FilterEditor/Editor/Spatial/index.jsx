import React from "react";
import PropTypes from "prop-types";

import { Button, ButtonGroup, Form, FormGroup, Input, Row, Col } from "reactstrap";

export { default as MapSelect } from "./MapSelect";

const SpatialFilter = ({ bounds, onChange, filters, deleteFilters }) => {
  const bBoxFilter = Object.keys(filters).filter(
    (key) => filters[key].remove === false && key === "bbox" && key !== "datetime"
  );
  const hasBboxInFilters = bBoxFilter.length > 0;

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    onChange(
      "bbox",
      `${bounds[0][0].toFixed(4)},${bounds[0][1].toFixed(4)},${bounds[1][0].toFixed(
        4
      )},${bounds[1][1].toFixed(4)}`
    );
  };

  return (
    <Form onSubmit={save}>
      <p className="text-muted text-uppercase">bbox</p>
      <Row>
        <Col md="5">
          <FormGroup>
            <Input
              type="text"
              size="sm"
              name="minLng"
              id="minLng"
              className="mr-2"
              value={bounds[0][0].toFixed(4)}
              readOnly
            />
          </FormGroup>
        </Col>
        <Col md="5">
          <FormGroup>
            <Input
              type="text"
              size="sm"
              name="minLat"
              id="minLat"
              className="mr-2"
              value={bounds[0][1].toFixed(4)}
              readOnly
            />
          </FormGroup>
        </Col>
      </Row>
      <Row>
        <Col md="5">
          <FormGroup>
            <Input
              type="text"
              size="sm"
              name="maxLng"
              id="maxLng"
              className="mr-2"
              value={bounds[1][0].toFixed(4)}
              readOnly
            />
          </FormGroup>
        </Col>
        <Col md="5">
          <FormGroup>
            <Input
              type="text"
              size="sm"
              name="maxLat"
              id="maxLat"
              className="mr-2"
              value={bounds[1][1].toFixed(4)}
              readOnly
            />
          </FormGroup>
        </Col>
        {hasBboxInFilters ? (
          <Row>
            <Col md="2">
              <ButtonGroup>
                <Button
                  color="primary"
                  size="sm"
                  style={{ width: "40px", height: "30px", left: "15px" }}
                  onClick={save}
                >
                  {"\u2713"}
                </Button>
                <Button
                  color="danger"
                  size="sm"
                  style={{ width: "40px", height: "30px", left: "15px" }}
                  onClick={deleteFilters("bbox")}
                >
                  {"\u2716"}
                </Button>
              </ButtonGroup>
            </Col>
          </Row>
        ) : (
          <Col md="2">
            <Button color="primary" size="sm" onClick={save}>
              Add
            </Button>
          </Col>
        )}
      </Row>
    </Form>
  );
};

SpatialFilter.displayName = "SpatialFilter";

SpatialFilter.propTypes = {
  bounds: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
  onChange: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  filters: PropTypes.object.isRequired,
  deleteFilters: PropTypes.func.isRequired,
};

SpatialFilter.defaultProps = {
  bounds: [
    [0, 0],
    [0, 0],
  ],
};

export default SpatialFilter;
