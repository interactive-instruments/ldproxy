import React, { useState } from "react";
import PropTypes from "prop-types";
import { Slider, Rail, Handles, Tracks, Ticks } from "react-compound-slider";
import { differenceInYears, format, differenceInMonths, differenceInHours } from "date-fns";
import { scaleTime } from "d3-scale";
import { SliderRail, Handle, Track, Tick } from "./components";

const halfHour = 1000 * 60 * 30;

const sliderStyle = {
  position: "relative",
  width: "100%",
};

function SliderPeriod({ isInstant, period, setPeriod, min, max }) {
  const [updated, setUpdated] = useState([period.start, period.end]);

  const formatTick = (ms) => {
    let dateFormat;
    if (differenceInYears(max, min) > 7) {
      dateFormat = format(new Date(ms), "yyyy");
    } else if (differenceInYears(max, min) > 3) {
      dateFormat = format(new Date(ms), "MMM yyyy");
    } else if (differenceInMonths(max, min) > 7) {
      dateFormat = format(new Date(ms), "MMM");
    } else if (differenceInHours(max, min) > 24) {
      dateFormat = format(new Date(ms), "MMM dd");
    } else if (differenceInHours(max, min) < 24) {
      dateFormat = format(new Date(ms), "HH:mm:ss");
    }
    return dateFormat;
  };

  const dateTicks = scaleTime()
    .domain([min, max])
    .ticks(8)
    .map((d) => +d);

  const onUpdate = (updatedValues) => {
    setPeriod((prevPeriod) => {
      return {
        ...prevPeriod,
        start: new Date(updatedValues[0]),
        end: new Date(updatedValues[1]),
      };
    });
    setUpdated([new Date(updatedValues[0]), new Date(updatedValues[1])]);
  };

  const renderDateTime = (date, header) => {
    const diffInMonths = differenceInMonths(max, min);
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
      {renderDateTime(updated, isInstant ? "Instant" : "Period")}
      <div style={{ margin: "5%", height: 120, width: "90%" }}>
        <Slider
          mode={1}
          step={halfHour}
          utc
          domain={[+min, +max]}
          rootStyle={sliderStyle}
          onUpdate={onUpdate}
          values={[+updated[0], +updated[1]]}
        >
          <Rail>{({ getRailProps }) => <SliderRail getRailProps={getRailProps} />}</Rail>
          <Handles>
            {({ handles, getHandleProps }) => (
              <div>
                {handles.map((handle) => (
                  <Handle
                    key={handle.id}
                    handle={handle}
                    domain={[+min, +max]}
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
                  <Track key={id} source={source} target={target} getTrackProps={getTrackProps} />
                ))}
              </div>
            )}
          </Tracks>

          <Ticks values={dateTicks}>
            {({ ticks }) => (
              <div>
                {ticks.map((tick) => (
                  <Tick key={tick.id} tick={tick} count={ticks.length} format={formatTick} />
                ))}
              </div>
            )}
          </Ticks>
        </Slider>
      </div>
    </div>
  );
}

export default SliderPeriod;

SliderPeriod.propTypes = {
  min: PropTypes.instanceOf(Date).isRequired,
  max: PropTypes.instanceOf(Date).isRequired,
  isInstant: PropTypes.bool.isRequired,
  setPeriod: PropTypes.func.isRequired,
  period: PropTypes.shape({
    start: PropTypes.instanceOf(Date).isRequired,
    end: PropTypes.instanceOf(Date),
  }).isRequired,
};
