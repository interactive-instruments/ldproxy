import React, { useState, useEffect } from "react";
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

const SpatialFilter = ({ bounds, setBounds, onChange, filters, deleteFilters }) => {
  const [minLng, setMinLng] = useState(Number(bounds[0][0]).toFixed(4));
  const [minLat, setMinLat] = useState(Number(bounds[0][1]).toFixed(4));
  const [maxLng, setMaxLng] = useState(Number(bounds[1][0]).toFixed(4));
  const [maxLat, setMaxLat] = useState(Number(bounds[1][1]).toFixed(4));

  const [isLngMinValid, setIsLngValid] = useState(true);
  const [isLatMinValid, setIsLatValid] = useState(true);
  const [isLngMaxValid, setIsLngMaxValid] = useState(true);
  const [isLatMaxValid, setIsLatMaxValid] = useState(true);

  const [LngMinLessMax, setMinLngCorrect] = useState(true);
  const [LatMinLessMax, setMinLatCorrect] = useState(true);

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

  const testMinLng = (lng) => {
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

  const testMinLat = (lat) => {
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

  useEffect(() => {
    setMinLng(Number(bounds[0][0]).toFixed(4));
    setMinLat(Number(bounds[0][1]).toFixed(4));
    setMaxLng(Number(bounds[1][0]).toFixed(4));
    setMaxLat(Number(bounds[1][1]).toFixed(4));

    testMinLng(Number(bounds[0][0]));
    testMaxLng(Number(bounds[1][0]));
    testMinLat(Number(bounds[0][1]));
    testMaxLat(Number(bounds[1][1]));
  }, [bounds]);

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
              className={LngMinLessMax && isLngMinValid ? "mr-2" : "mr-2 is-invalid"}
              value={Number(bounds[0][0]) || ""}
              onChange={(e) => {
                testMinMaxLng(Number(e.target.value), maxLng);
                testMinLng(e.target.value);
                setBounds([
                  [Number(e.target.value).toFixed(4), Number(minLat).toFixed(4)],
                  [Number(maxLng).toFixed(4), Number(maxLat).toFixed(4)],
                ]);
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngMinValid &&
                  isLatMinValid &&
                  LngMinLessMax &&
                  LatMinLessMax &&
                  isLngMaxValid &&
                  isLatMaxValid
                ) {
                  save(event);
                }
              }}
            />
            {LngMinLessMax && isLngMinValid && <FormText>Min. Longitude</FormText>}
            {!LngMinLessMax && <FormFeedback>Min. greater than Max.</FormFeedback>}
            {!isLngMinValid && <FormFeedback>Value too low/high for Lng</FormFeedback>}
          </FormGroup>
        </Col>
        <Col md="5">
          <FormGroup>
            <Input
              type="number"
              size="sm"
              name="minLat"
              id="minLat"
              className={isLatMinValid && LatMinLessMax ? "mr-2" : "mr-2 is-invalid"}
              value={Number(bounds[0][1]) || ""}
              onChange={(e) => {
                testMinMaxLat(Number(e.target.value), maxLat);
                testMinLat(e.target.value);
                setBounds([
                  [Number(minLng).toFixed(4), Number(e.target.value).toFixed(4)],
                  [Number(maxLng).toFixed(4), Number(maxLat).toFixed(4)],
                ]);
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngMinValid &&
                  isLatMinValid &&
                  LngMinLessMax &&
                  LatMinLessMax &&
                  isLngMaxValid &&
                  isLatMaxValid
                ) {
                  save(event);
                }
              }}
            />
            {LatMinLessMax && isLatMinValid && <FormText>Min. Latitude</FormText>}
            {!LatMinLessMax && <FormFeedback>Min. greater than Max.</FormFeedback>}
            {!isLatMinValid && <FormFeedback>Value too low/high for Lat</FormFeedback>}
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
              className={isLngMaxValid && LngMinLessMax ? "mr-2" : "mr-2 is-invalid"}
              value={Number(bounds[1][0]) || ""}
              onChange={(e) => {
                testMinMaxLng(minLng, Number(e.target.value));
                testMaxLng(e.target.value);
                setBounds([
                  [Number(minLng).toFixed(4), Number(minLat).toFixed(4)],
                  [Number(e.target.value).toFixed(4), Number(maxLat).toFixed(4)],
                ]);
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngMinValid &&
                  isLatMinValid &&
                  LngMinLessMax &&
                  LatMinLessMax &&
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
              className={isLatMaxValid && LatMinLessMax ? "mr-2" : "mr-2 is-invalid"}
              value={Number(bounds[1][1]) || ""}
              onChange={(e) => {
                testMinMaxLat(minLat, Number(e.target.value));
                testMaxLat(e.target.value);
                setBounds([
                  [Number(minLng).toFixed(4), Number(minLat).toFixed(4)],
                  [Number(maxLng).toFixed(4), Number(e.target.value).toFixed(4)],
                ]);
              }}
              onKeyPress={(event) => {
                if (
                  event.key === "Enter" &&
                  isLngMinValid &&
                  isLatMinValid &&
                  LngMinLessMax &&
                  LatMinLessMax &&
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
                  !isLngMinValid ||
                  !isLatMinValid ||
                  !LngMinLessMax ||
                  !LatMinLessMax ||
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
                !isLngMinValid ||
                !isLatMinValid ||
                !LngMinLessMax ||
                !LatMinLessMax ||
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
  bounds: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)),
  onChange: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  filters: PropTypes.object.isRequired,
  deleteFilters: PropTypes.func.isRequired,
  setBounds: PropTypes.func.isRequired,
};

SpatialFilter.defaultProps = {
  bounds: [
    [0, 0],
    [0, 0],
  ],
};

export default SpatialFilter;
