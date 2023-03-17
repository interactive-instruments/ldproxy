import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import qs from "qs";
import { startOfToday } from "date-fns";
import { Button, ButtonGroup, Form, Input, Row, Col, FormText } from "reactstrap";
import DatetimeRangePicker from "react-datetime-range-picker";
import Datetime from "react-datetime";
import moment from "moment";
import SliderInstant from "./Slider";

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
  const min = start;
  const max = end;

  const minInstant = start;
  const maxInstant = end !== null ? end : startOfToday();

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
  const [instantInput, setInstantInput] = useState(new Date(extent.start));
  const [period, setPeriod] = useState({
    start: new Date(extent.start),
    end: new Date(extent.end ? extent.end : extent.start),
  });
  const [periodInput, setPeriodInput] = useState({
    start: new Date(extent.start),
    end: new Date(extent.end ? extent.end : extent.start),
  });
  const [isInstant, setIsInstant] = useState(extent.end === null);
  const [userInputValidation, setUserInputValidation] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const [isStartValid, setIsStartValid] = useState(true);
  const [isEndValid, setIsEndValid] = useState(true);
  const [startLessEnd, setStartLessEnd] = useState(true);
  const [endGreaterStart, setEndGreaterStart] = useState(true);

  useEffect(() => {
    const parsedQuery = qs.parse(window.location.search, {
      ignoreQueryPrefix: true,
    });
    if (parsedQuery.datetime) {
      const datetime = fromFilterString(parsedQuery.datetime);
      if (datetime.end === null) {
        setInstant(moment.utc(datetime.start).toDate());
      } else {
        setPeriod({
          start: moment.utc(datetime.start).toDate(),
          end: moment.utc(datetime.end).toDate(),
        });
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

  const error = (inputValue) => {
    const parsedDate = moment.utc(inputValue, "DD.MM.YYYY HH:mm:ss", true);
    if (!parsedDate.isValid()) {
      setErrorMessage("Invalid date format.");
    } else if (!parsedDate.isSameOrAfter(moment.utc(min))) {
      setErrorMessage("Date is before the minimum date.");
    } else if (!parsedDate.isSameOrBefore(moment.utc(max))) {
      setErrorMessage("Date is after the maximum date.");
    } else if (!parsedDate.isSameOrBefore(moment.utc())) {
      setErrorMessage("Date is in the future.");
    } else {
      setErrorMessage("");
    }
    return errorMessage;
  };

  const testFunction = (inputValue) => {
    error(inputValue);
    const parsedDate = moment.utc(inputValue, "DD.MM.YYYY HH:mm:ss", true);
    if (
      parsedDate.isValid() &&
      parsedDate.isSameOrAfter(moment.utc(min)) &&
      parsedDate.isSameOrBefore(moment.utc(max)) &&
      parsedDate.isSameOrBefore(moment.utc())
    ) {
      setUserInputValidation(true);
      return true;
    }
    setUserInputValidation(false);
    return false;
  };

  const testStart = (inputValue) => {
    const parsedDate = moment.utc(inputValue, "DD.MM.YYYY HH:mm:ss", true);
    if (
      parsedDate.isValid() &&
      parsedDate.isSameOrAfter(moment.utc(min)) &&
      parsedDate.isSameOrBefore(moment.utc(max))
    ) {
      setIsStartValid(true);
      return true;
    }
    setIsStartValid(false);
    return false;
  };
  // eslint-disable-next-line
  const testStartLessEnd = (next) => {
    const parsedDate = moment.utc(next, "DD.MM.YYYY HH:mm:ss", true);
    if (parsedDate.isSameOrBefore(moment.utc(period.end))) {
      setStartLessEnd(true);
      return true;
    }
    setStartLessEnd(false);
  };

  const testEnd = (inputValue) => {
    const parsedDate = moment.utc(inputValue, "DD.MM.YYYY HH:mm:ss", true);
    if (
      parsedDate.isValid() &&
      parsedDate.isSameOrBefore(moment.utc(max)) &&
      parsedDate.isSameOrAfter(moment.utc(min))
    ) {
      setIsEndValid(true);
      return true;
    }
    setIsEndValid(false);
    return false;
  };

  const testEndGreaterStart = (next) => {
    const parsedDate = moment.utc(next, "DD.MM.YYYY HH:mm:ss", true);
    if (parsedDate.isSameOrAfter(moment.utc(period.start))) {
      setEndGreaterStart(true);
      return true;
    }
    setEndGreaterStart(false);
    return false;
  };

  useEffect(() => {
    if (isInstant) {
      setInstantInput(instant);
      testFunction(instant);
      error(instant);
    } else {
      setPeriodInput({
        start: period.start,
        end: period.end,
      });
      testEnd(period.end);
      testStart(period.start);
      testEndGreaterStart(period.end);
      testStartLessEnd(period.start);
    }
  }, [instant, period]);
  return (
    <Form onSubmit={save}>
      <p className="text-muted text-uppercase">date/time (utc)</p>
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
            {!userInputValidation && <FormText>{errorMessage}</FormText>}
            <Datetime
              className=""
              inputProps={{
                className: userInputValidation
                  ? "form-control form-control-sm w-100 mb-3"
                  : "form-control form-control-sm w-100 mb-3 is-invalid",
                style: {
                  backgroundColor: "white",
                  cursor: "pointer",
                },
              }}
              timeFormat="HH:mm:ss"
              dateFormat="DD.MM.YYYY"
              utc
              value={instantInput}
              onChange={(next) => {
                setInstantInput(next);
                const isValidInput = testFunction(next);
                if (isValidInput) {
                  setInstant(next);
                  setIsInstant(true);
                }
              }}
              onKeyPress={(event) => {
                if (event.key === "Enter" && userInputValidation) {
                  save(event);
                }
              }}
            />

            <Input size="sm" className="mb-3" disabled />
          </Col>
        ) : (
          <>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                marginBottom: "10px",
              }}
            >
              {!isStartValid && (
                <FormText style={{ marginLeft: "15px" }}>
                  The start date is outside the specified range.
                </FormText>
              )}
              {!isEndValid && (
                <FormText style={{ marginLeft: "15px" }}>
                  The end date is outside the specified range.
                </FormText>
              )}
              {!startLessEnd && (
                <FormText style={{ marginLeft: "15px" }}>
                  The start date must be less than the end date.
                </FormText>
              )}
              {!endGreaterStart && (
                <FormText style={{ marginLeft: "15px" }}>
                  The end date must be greater than the start date.
                </FormText>
              )}
            </div>

            <DatetimeRangePicker
              className="col-md-10"
              input
              inputProps={{
                input: true,
                inputProps: {
                  className:
                    userInputValidation &&
                    isStartValid &&
                    isEndValid &&
                    startLessEnd &&
                    endGreaterStart
                      ? "form-control form-control-sm w-100 mb-3"
                      : "form-control form-control-sm w-100 mb-3 is-invalid",
                },
              }}
              timeFormat="HH:mm:ss"
              dateFormat="DD.MM.YYYY"
              utc
              startDate={periodInput.start}
              endDate={periodInput.end}
              onStartDateChange={(next) => {
                setPeriodInput(() => ({
                  start: next,
                  end: periodInput.end,
                }));
                const isValidInput = testStart(next);
                const minMaxOk = testStartLessEnd(next);
                if (isValidInput && minMaxOk) {
                  setPeriod((prevPeriod) => ({
                    ...prevPeriod,
                    start: next,
                  }));
                  setIsInstant(false);
                }
              }}
              onEndDateChange={(next) => {
                setPeriodInput(() => ({
                  start: periodInput.start,
                  end: next,
                }));
                const isValidInput = testEnd(next);
                const minMaxOk = testEndGreaterStart(next);
                if (isValidInput && minMaxOk) {
                  setPeriod((prevPeriod) => ({
                    ...prevPeriod,
                    end: next,
                  }));
                  setIsInstant(false);
                }
              }}
            />
          </>
        )}
        {hasDateTimeInFilters ? (
          <Col md="2" className="d-flex align-items-end mb-3">
            <ButtonGroup>
              <Button
                color="primary"
                size="sm"
                style={{ minWidth: "40px" }}
                onClick={save}
                disabled={
                  !userInputValidation ||
                  !isStartValid ||
                  !isEndValid ||
                  !startLessEnd ||
                  !endGreaterStart
                }
              >
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
            <Button
              color="primary"
              size="sm"
              onClick={save}
              disabled={
                !userInputValidation ||
                !isStartValid ||
                !isEndValid ||
                !startLessEnd ||
                !endGreaterStart
              }
            >
              Add
            </Button>
          </Col>
        )}
      </Row>
      {min !== max && (
        <>
          <Row>
            <Col md="10">
              <SliderInstant
                period={period}
                setInstant={setInstant}
                minInstant={minInstant}
                maxInstant={maxInstant}
                instant={instant}
                isInstant={isInstant}
                setPeriod={setPeriod}
                min={min}
                max={max}
              />
            </Col>
          </Row>
        </>
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
