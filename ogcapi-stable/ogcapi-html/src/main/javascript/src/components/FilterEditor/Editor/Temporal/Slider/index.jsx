import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import moment from "moment";
import { Slider, Rail, Handles, Tracks, Ticks } from "react-compound-slider";
import { differenceInYears, format, differenceInMonths, differenceInHours } from "date-fns";
import { scaleTime } from "d3-scale";
import { SliderRail, Handle, Track, Tick } from "../Components";

const sliderStyle = {
  position: "relative",
  width: "100%",
};

function SliderInstant({
  setPeriod,
  min,
  max,
  isInstant,
  minInstant,
  maxInstant,
  setInstant,
  forStory,
  instant,
  period,
}) {
  const [updatedInstant, setUpdatedInstant] = useState(instant);
  const [updatedPeriod, setUpdatedPeriod] = useState([period.start, moment(period.end).valueOf()]);
  const maxInstantMinusOneHour = moment(maxInstant).subtract(1, "hour").valueOf();
  const maxMinusOneHourPeriod = moment(max).subtract(1, "hour").valueOf();

  const numSteps = 100;
  const rangeInstant = maxInstantMinusOneHour - minInstant;
  const stepInstant = rangeInstant / numSteps;
  const rangePeriod = maxMinusOneHourPeriod - min;
  const stepPeriod = rangePeriod / numSteps;

  const formatTickInstant = (ms) => {
    let dateFormat;
    if (differenceInYears(maxInstantMinusOneHour, minInstant) > 7) {
      dateFormat = format(new Date(ms), "yyyy");
    } else if (differenceInYears(maxInstantMinusOneHour, minInstant) > 3) {
      dateFormat = format(new Date(ms), "MMM yyyy");
    } else if (differenceInMonths(maxInstantMinusOneHour, minInstant) > 7) {
      dateFormat = format(new Date(ms), "MMM");
    } else if (differenceInHours(maxInstantMinusOneHour, minInstant) > 24) {
      dateFormat = format(new Date(ms), "dd MMM");
    } else if (differenceInHours(maxInstantMinusOneHour, minInstant) < 24) {
      dateFormat = format(new Date(ms), "HH:mm:ss");
    }
    return dateFormat;
  };

  const dateTicksInstant = scaleTime()
    .domain([minInstant, maxInstantMinusOneHour])
    .ticks(8)
    .map((d) => +d);

  const onUpdateInstant = ([ms]) => {
    // setUpdated(new Date(ms));
    setInstant(moment(ms).utc(true));
  };

  useEffect(() => {
    const steps = [...Array(numSteps).keys()].map((i) => minInstant + i * stepInstant);
    const closestStep = steps.reduce((prev, curr) =>
      Math.abs(curr - instant) < Math.abs(prev - instant) ? curr : prev
    );
    setUpdatedInstant(closestStep);
  }, [instant]);

  // renderDateTime is only used for Storybook
  const renderDateTimeInstant = (date, header) => {
    const diffInMonths = differenceInMonths(maxInstantMinusOneHour, minInstant);
    const formattedDate =
      diffInMonths > 1 ? format(date, "dd.MM.yyyy") : format(date, "dd.MM.yyyy HH:mm:ss");

    return (
      <div
        style={{
          width: "100%",
          textAlign: "center",
          fontFamily: "Arial",
          margin: 5,
        }}
      >
        <b>{header}:</b>
        <div style={{ fontSize: 12 }}>{formattedDate}</div>
      </div>
    );
  };

  const formatTickPeriod = (ms) => {
    let dateFormat;
    if (differenceInYears(maxMinusOneHourPeriod, min) > 7) {
      dateFormat = format(new Date(ms), "yyyy");
    } else if (differenceInYears(maxMinusOneHourPeriod, min) > 3) {
      dateFormat = format(new Date(ms), "MMM yyyy");
    } else if (differenceInMonths(maxMinusOneHourPeriod, min) > 7) {
      dateFormat = format(new Date(ms), "MMM");
    } else if (differenceInHours(maxMinusOneHourPeriod, min) > 24) {
      dateFormat = format(new Date(ms), "dd MMM");
    } else if (differenceInHours(maxMinusOneHourPeriod, min) < 24) {
      dateFormat = format(new Date(ms), "HH:mm:ss");
    }
    return dateFormat;
  };

  const dateTicksPeriod = scaleTime()
    .domain([min, maxMinusOneHourPeriod])
    .ticks(8)
    .map((d) => +d);

  const onUpdatePeriod = (updatedValues) => {
    setUpdatedPeriod([new Date(updatedValues[0]), new Date(updatedValues[1])]);
    setPeriod((prevPeriod) => {
      return {
        ...prevPeriod,
        start: new Date(moment(updatedValues[0]).utc(true)),
        end: new Date(moment(updatedValues[1]).utc(true)),
      };
    });
  };

  useEffect(() => {
    const steps = [...Array(numSteps).keys()].map((i) => min + i * stepPeriod);
    const closestStepStart = steps.reduce((prev, curr) =>
      Math.abs(curr - period.start) < Math.abs(prev - period.start) ? curr : prev
    );
    const closestStepEnd = steps.reduce((prev, curr) =>
      Math.abs(curr - period.end) < Math.abs(prev - period.end) ? curr : prev
    );
    setUpdatedPeriod([closestStepStart, closestStepEnd]);
  }, [period]);

  // renderDateTime is only used for Storybook
  const renderDateTimePeriod = (date, header) => {
    const diffInMonths = differenceInMonths(maxMinusOneHourPeriod, min);
    const formattedDateStart =
      diffInMonths > 1 ? format(date[0], "dd.MM.yyyy") : format(date[0], "dd.MM.yyyy HH:mm:ss");
    const formattedDateEnd =
      diffInMonths > 1 ? format(date[1], "dd.MM.yyyy") : format(date[1], "dd.MM.yyyy HH:mm:ss");

    return (
      <div
        style={{
          width: "100%",
          textAlign: "center",
          fontFamily: "Arial",
          margin: 5,
        }}
      >
        <b>{header}:</b>
        <div style={{ fontSize: 12 }}>
          {formattedDateStart} -- {formattedDateEnd}
        </div>
      </div>
    );
  };

  return (
    <div>
      {isInstant ? (
        <div>
          {forStory && renderDateTimeInstant(instant, "Date/Time")}
          <div style={{ margin: "10px", height: 120 }}>
            <Slider
              mode={1}
              step={stepInstant}
              domain={[+minInstant, +maxInstantMinusOneHour]}
              rootStyle={sliderStyle}
              onUpdate={onUpdateInstant}
              values={[+updatedInstant]}
            >
              <Rail>{({ getRailProps }) => <SliderRail getRailProps={getRailProps} />}</Rail>
              <Handles>
                {({ handles, getHandleProps }) => (
                  <div>
                    {handles.map((handle) => (
                      <Handle
                        key={handle.id}
                        handle={handle}
                        domain={[+minInstant, +maxInstantMinusOneHour]}
                        getHandleProps={getHandleProps}
                      />
                    ))}
                  </div>
                )}
              </Handles>
              <Tracks right={false} left={false}>
                {({ tracks, getTrackProps }) => (
                  <div>
                    {tracks.map(({ id, source, target }) => (
                      <Track
                        key={id}
                        source={source}
                        target={target}
                        getTrackProps={getTrackProps}
                      />
                    ))}
                  </div>
                )}
              </Tracks>

              <Ticks values={dateTicksInstant}>
                {({ ticks }) => (
                  <div>
                    {ticks.map((tick) => (
                      <Tick
                        key={tick.id}
                        tick={tick}
                        count={ticks.length}
                        format={formatTickInstant}
                      />
                    ))}
                  </div>
                )}
              </Ticks>
            </Slider>
          </div>
        </div>
      ) : (
        <div>
          {forStory && renderDateTimePeriod(updatedPeriod, "Date/Time")}
          <div style={{ margin: "10px", height: 120 }}>
            <Slider
              mode={1}
              step={stepPeriod}
              utc
              domain={[+min, +maxMinusOneHourPeriod]}
              rootStyle={sliderStyle}
              onUpdate={onUpdatePeriod}
              values={[+updatedPeriod[0], +updatedPeriod[1]]}
            >
              <Rail>{({ getRailProps }) => <SliderRail getRailProps={getRailProps} />}</Rail>
              <Handles>
                {({ handles, getHandleProps }) => (
                  <div>
                    {handles.map((handle) => (
                      <Handle
                        key={handle.id}
                        handle={handle}
                        domain={[+min, +maxMinusOneHourPeriod]}
                        getHandleProps={getHandleProps}
                      />
                    ))}
                  </div>
                )}
              </Handles>
              <Tracks left={false} right={false}>
                {({ tracks, getTrackProps }) => (
                  <div>
                    {tracks.map(({ id, source, target }) => (
                      <Track
                        key={id}
                        source={source}
                        target={target}
                        getTrackProps={getTrackProps}
                      />
                    ))}
                  </div>
                )}
              </Tracks>

              <Ticks values={dateTicksPeriod}>
                {({ ticks }) => (
                  <div>
                    {ticks.map((tick) => (
                      <Tick
                        key={tick.id}
                        tick={tick}
                        count={ticks.length}
                        format={formatTickPeriod}
                      />
                    ))}
                  </div>
                )}
              </Ticks>
            </Slider>
          </div>
        </div>
      )}
    </div>
  );
}

SliderInstant.propTypes = {
  min: PropTypes.number.isRequired,
  max: PropTypes.number.isRequired,
  setPeriod: PropTypes.func.isRequired,
  minInstant: PropTypes.number.isRequired,
  maxInstant: PropTypes.number.isRequired,
  setInstant: PropTypes.func.isRequired,
  period: PropTypes.shape({
    start: PropTypes.instanceOf(Date).isRequired,
    end: PropTypes.instanceOf(Date),
  }).isRequired,
  forStory: PropTypes.bool,
  instant: PropTypes.number.isRequired,
  isInstant: PropTypes.bool.isRequired,
};

SliderInstant.defaultProps = {
  forStory: false,
};

export default SliderInstant;
