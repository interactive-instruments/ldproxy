import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import qs from "qs";

import { Button, Form, FormGroup, Input, Row, Col } from "reactstrap";

export { default as MapSelect } from "./MapSelect";

const SpatialFilter = ({ bounds, onChange }) => {
  // eslint-disable-next-line
  const [boundsChanged, setBoundsChanged] = useState(bounds);

  useEffect(() => {
    const parsedQuery = qs.parse(window.location.search, {
      ignoreQueryPrefix: true,
    });
    if (parsedQuery.boundsChanged) {
      const [minLng, minLat, maxLng, maxLat] = parsedQuery.boundsChanged.split(",");
      setBoundsChanged([
        [Number(minLng), Number(minLat)],
        [Number(maxLng), Number(maxLat)],
      ]);
    }
  }, []);

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    onChange(
      "bbox",
      `${bounds[0][0].toFixed(4)},${bounds[0][1].toFixed(4)},${bounds[1][0].toFixed(
        4
      )},${bounds[1][1].toFixed(4)}`
    );

    const parsedQuery = qs.parse(window.location.search, {
      ignoreQueryPrefix: true,
    });
    const newQuery = qs.stringify({ ...parsedQuery, boundsChanged: onChange });
    window.history.replaceState(null, "", `?${newQuery}`);
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
        <Col md="2">
          <Button color="primary" size="sm" onClick={save}>
            Add
          </Button>
        </Col>
      </Row>
    </Form>
  );
};

SpatialFilter.displayName = "SpatialFilter";

SpatialFilter.propTypes = {
  bounds: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
  onChange: PropTypes.func.isRequired,
};

SpatialFilter.defaultProps = {
  bounds: [
    [0, 0],
    [0, 0],
  ],
};

export default SpatialFilter;
