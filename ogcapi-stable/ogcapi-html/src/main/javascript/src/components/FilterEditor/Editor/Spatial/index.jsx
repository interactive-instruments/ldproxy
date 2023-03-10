import React, { useState } from "react";
import PropTypes from "prop-types";

import {
  Button,
  ButtonGroup,
  Form,
  FormGroup,
  Input,
  Row,
  Col,
  FormFeedback,
  FormText,
} from "reactstrap";

export { default as MapSelect } from "./MapSelect";

const SpatialFilter = ({ bounds, onChange, filters, deleteFilters }) => {
  const [minLng, setMinLng] = useState(Number(bounds[0][0]).toFixed(4));
  const [minLat, setMinLat] = useState(Number(bounds[0][1]).toFixed(4));
  const [maxLng, setMaxLng] = useState(Number(bounds[1][0]).toFixed(4));
  const [maxLat, setMaxLat] = useState(Number(bounds[1][1]).toFixed(4));

  const [isLngValid, setIsLngValid] = useState(true);
  const [isLatValid, setIsLatValid] = useState(true);
  const [isLngMaxValid, setIsLngMaxValid] = useState(true);
  const [isLatMaxValid, setIsLatMaxValid] = useState(true);

  const [minLngCorrect, setMinLngCorrect] = useState(true);
  const [minLatCorrect, setMinLatCorrect] = useState(true);

  const bBoxFilter = Object.keys(filters).filter(
    (key) => filters[key].remove === false && key === "bbox" && key !== "datetime"
  );
  const hasBboxInFilters = bBoxFilter.length > 0;

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    const bboxInput = `${parseFloat(Math.round(minLng * 10000) / 10000).toFixed(4)},${parseFloat(
      Math.round(minLat * 10000) / 10000
    ).toFixed(4)},${parseFloat(Math.round(maxLng * 10000) / 10000).toFixed(4)},${parseFloat(
      Math.round(maxLat * 10000) / 10000
    ).toFixed(4)}`;

    onChange("bbox", bboxInput);
  };

  const testLng = (lng) => {
    let isValid = true;
    if (lng < -180 || lng > 180) isValid = false;
    setIsLngValid(isValid);
    return isValid;
  };

  const testMaxLng = (lngMax) => {
    let isValid = true;
    if (lngMax < -180 || lngMax > 180) isValid = false;
    setIsLngMaxValid(isValid);
    return isValid;
  };

  const testLat = (lat) => {
    let isValid = true;
    if (lat < -90 || lat > 90) isValid = false;
    setIsLatValid(isValid);
    return isValid;
  };

  const testMaxLat = (latMax) => {
    let isValid = true;
    if (latMax < -90 || latMax > 90) isValid = false;
    setIsLatMaxValid(isValid);
    return isValid;
  };

  const testMinMaxLng = (min, max) => {
    const isValid = min <= max;
    setMinLngCorrect(isValid);
    return isValid;
  };

  const testMinMaxLat = (min, max) => {
    const isValid = min <= max;
    setMinLatCorrect(isValid);
    return isValid;
  };

  return (
    <Form onSubmit={save}>
      <p className="text-muted text-uppercase">bbox</p>
      <Row>
        <Col md="5">
          <FormGroup>
            <Input
              type="number"
              size="sm"
              name="minLng"
              id="minLng"
              className={minLngCorrect && isLngValid ? "mr-2" : "mr-2 is-invalid"}
              defaultValue={minLng}
              onChange={(e) => {
                const minMaxValid = testMinMaxLng(Number(e.target.value), maxLng);
                const isValidInputLng = testLng(e.target.value);
                if (isValidInputLng && minMaxValid) {
                  setMinLng(Number(e.target.value));
                }
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngValid &&
                  isLatValid &&
                  minLngCorrect &&
                  minLatCorrect &&
                  isLngMaxValid &&
                  isLatMaxValid
                ) {
                  save(event);
                }
              }}
            />
            {minLngCorrect && isLngValid && <FormText>Min. Longitude</FormText>}
            {!minLngCorrect && <FormFeedback>Min greater than Max!</FormFeedback>}
            {!isLngValid && <FormFeedback>Value too low/high for Lng</FormFeedback>}
          </FormGroup>
        </Col>
        <Col md="5">
          <FormGroup>
            <Input
              type="number"
              size="sm"
              name="minLat"
              id="minLat"
              className={isLatValid && minLatCorrect ? "mr-2" : "mr-2 is-invalid"}
              defaultValue={minLat}
              onChange={(e) => {
                const minMaxValid = testMinMaxLat(Number(e.target.value), maxLat);
                const isValidInputLat = testLat(e.target.value);
                if (isValidInputLat && minMaxValid) {
                  setMinLat(Number(e.target.value));
                }
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngValid &&
                  isLatValid &&
                  minLngCorrect &&
                  minLatCorrect &&
                  isLngMaxValid &&
                  isLatMaxValid
                ) {
                  save(event);
                }
              }}
            />
            {minLatCorrect && isLatValid && <FormText>Min. Latitude</FormText>}
            {!minLatCorrect && <FormFeedback>Min greater than Max!</FormFeedback>}
            {!isLatValid && <FormFeedback>Value too low/high for Lat</FormFeedback>}
          </FormGroup>
        </Col>
      </Row>
      <Row>
        <Col md="5">
          <FormGroup>
            <Input
              type="number"
              size="sm"
              name="maxLng"
              id="maxLng"
              className={isLngMaxValid && minLngCorrect ? "mr-2" : "mr-2 is-invalid"}
              defaultValue={maxLng}
              onChange={(e) => {
                const minMaxValid = testMinMaxLng(minLng, Number(e.target.value));
                const isValidInputLng = testMaxLng(e.target.value);
                if (isValidInputLng && minMaxValid) {
                  setMaxLng(Number(e.target.value));
                }
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngValid &&
                  isLatValid &&
                  minLngCorrect &&
                  minLatCorrect &&
                  isLngMaxValid &&
                  isLatMaxValid
                ) {
                  save(event);
                }
              }}
            />
            {isLngMaxValid && <FormText>Max. Longitude</FormText>}
            {!isLngMaxValid && <FormFeedback>Value too low/high for Lng</FormFeedback>}
          </FormGroup>
        </Col>
        <Col md="5">
          <FormGroup>
            <Input
              type="number"
              size="sm"
              name="maxLat"
              id="maxLat"
              className={isLatMaxValid && minLatCorrect ? "mr-2" : "mr-2 is-invalid"}
              defaultValue={maxLat}
              onChange={(e) => {
                const minMaxValid = testMinMaxLat(minLat, Number(e.target.value));
                const isValidInputLat = testMaxLat(e.target.value);
                if (isValidInputLat && minMaxValid) {
                  setMaxLat(Number(e.target.value));
                }
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngValid &&
                  isLatValid &&
                  minLngCorrect &&
                  minLatCorrect &&
                  isLngMaxValid &&
                  isLatMaxValid
                ) {
                  save(event);
                }
              }}
            />
            {isLatMaxValid && <FormText>Max. Latitude</FormText>}
            {!isLatMaxValid && <FormFeedback>Value too low/high for Lat</FormFeedback>}
          </FormGroup>
        </Col>
        {hasBboxInFilters ? (
          <Col md="2">
            <ButtonGroup>
              <Button
                color="primary"
                size="sm"
                style={{ minWidth: "40px" }}
                disabled={
                  !isLngValid ||
                  !isLatValid ||
                  !minLngCorrect ||
                  !minLatCorrect ||
                  !isLngMaxValid ||
                  !isLatMaxValid
                }
                onClick={save}
              >
                {"\u2713"}
              </Button>
              <Button
                color="danger"
                size="sm"
                style={{ minWidth: "40px" }}
                onClick={deleteFilters("bbox")}
              >
                {"\u2716"}
              </Button>
            </ButtonGroup>
          </Col>
        ) : (
          <Col md="2">
            <Button
              color="primary"
              size="sm"
              disabled={
                !isLngValid ||
                !isLatValid ||
                !minLngCorrect ||
                !minLatCorrect ||
                !isLngMaxValid ||
                !isLatMaxValid
              }
              onClick={save}
            >
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
