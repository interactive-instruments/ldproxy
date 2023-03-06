import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import qs from "qs";

import { Button, ButtonGroup, Form, Input, Row, Col } from "reactstrap";
import DatetimeRangePicker from "react-datetime-range-picker";
import Datetime from "react-datetime";
import moment from "moment";
import SliderFunction from "./sliderFunction";

const fromFilterString = (filter) => {
  if (filter.indexOf("/") === -1) {
    return {
      start: filter,
      end: null,
    };
  }

  return {
    start: filter.split("/")[0],
    end:
      filter.split("/")[1].indexOf("P") === 0
        ? moment
            .utc(filter.split("/")[0])
            .add(moment.duration(filter.split("/")[1]))
            .format()
        : filter.split("/")[1],
  };
};

export const toTimeLabel = (filter) => {
  const datetime = fromFilterString(filter);

  if (!datetime.end) {
    return `datetime=${moment.utc(datetime.start).format("DD.MM.YY HH:mm:ss")}`;
  }
  return `datetime=${moment.utc(datetime.start).format("DD.MM.YY HH:mm:ss")} - ${moment
    .utc(datetime.end)
    .format("DD.MM.YY HH:mm:ss")}`;
};

const formatDate = (date) => {
  return moment.utc(date).format();
};

const TemporalFilter = ({ start, end, filter, onChange, filters, deleteFilters }) => {
  const dateTimeFilter = Object.keys(filters).filter(
    (key) => filters[key].remove === false && key === "datetime"
  );
  const hasDateTimeInFilters = dateTimeFilter.length > 0;

  const extent = filter
    ? fromFilterString(filter)
    : {
        start,
        end,
      };

  const [instant, setInstant] = useState(new Date(extent.start));
  const [period, setPeriod] = useState({
    start: new Date(extent.start),
    end: new Date(extent.end ? extent.end : extent.start),
  });
  const [isInstant, setIsInstant] = useState(extent.end === null);

  useEffect(() => {
    const parsedQuery = qs.parse(window.location.search, {
      ignoreQueryPrefix: true,
    });
    if (parsedQuery.datetime) {
      const datetime = fromFilterString(parsedQuery.datetime);
      if (datetime.end === null) {
        setInstant(moment.utc(datetime.start).toDate());
        setIsInstant(true);
      } else {
        setPeriod({
          start: moment.utc(datetime.start).toDate(),
          end: moment.utc(datetime.end).toDate(),
        });
        setIsInstant(false);
      }
    }
  }, [filter]);

  const save = (event) => {
    event.preventDefault();
    event.stopPropagation();

    onChange(
      "datetime",
      isInstant ? formatDate(instant) : `${formatDate(period.start)}/${formatDate(period.end)}`
    );
  };

  return (
    <Form onSubmit={save}>
      <p className="text-muted text-uppercase">date/time</p>
      <ButtonGroup className="mb-3">
        <Button
          color="primary"
          outline={isInstant}
          size="sm"
          className="py-0"
          onClick={() => setIsInstant(false)}
        >
          Period
        </Button>
        <Button
          color="primary"
          outline={!isInstant}
          size="sm"
          className="py-0"
          onClick={() => setIsInstant(true)}
        >
          Instant
        </Button>
      </ButtonGroup>
      <Row>
        {isInstant ? (
          <Col md="10">
            <Datetime
              className=""
              inputProps={{
                className: "form-control form-control-sm w-100 mb-3",
                readOnly: true,
                style: {
                  backgroundColor: "white",
                  cursor: "pointer",
                },
              }}
              timeFormat="HH:mm:ss"
              dateFormat="DD.MM.YYYY"
              utc
              value={instant}
              onChange={(next) => setInstant(next)}
            />
            <Input size="sm" className="mb-3" disabled />
          </Col>
        ) : (
          <DatetimeRangePicker
            className="col-md-10"
            inputProps={{
              className: "form-control form-control-sm w-100 mb-3",
              readOnly: true,
            }}
            timeFormat="HH:mm:ss"
            dateFormat="DD.MM.YYYY"
            utc
            startDate={period.start}
            endDate={period.end}
            onChange={(next) => setPeriod(next)}
          />
        )}
        {hasDateTimeInFilters ? (
          <Col md="2" className="d-flex align-items-end mb-3">
            <ButtonGroup>
              <Button color="primary" size="sm" style={{ minWidth: "40px" }} onClick={save}>
                {"\u2713"}
              </Button>
              <Button
                color="danger"
                size="sm"
                style={{ minWidth: "40px" }}
                onClick={deleteFilters("datetime")}
              >
                {"\u2716"}
              </Button>
            </ButtonGroup>
          </Col>
        ) : (
          <Col md="2" className="d-flex align-items-end mb-3">
            <Button color="primary" size="sm" onClick={save}>
              Add
            </Button>
          </Col>
        )}
      </Row>
      {extent.start && (
        <Col md="10">
          <SliderFunction
            Instant={instant}
            period={period}
            isInstant={isInstant}
            setInstant={setInstant}
            setPeriod={setPeriod}
          />
        </Col>
      )}
    </Form>
  );
};

TemporalFilter.displayName = "TemporalFilter";

TemporalFilter.propTypes = {
  start: PropTypes.number.isRequired,
  end: PropTypes.number,
  filter: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  filters: PropTypes.object.isRequired,
  deleteFilters: PropTypes.func.isRequired,
};

TemporalFilter.defaultProps = {
  end: null,
  filter: null,
};

export default TemporalFilter;
