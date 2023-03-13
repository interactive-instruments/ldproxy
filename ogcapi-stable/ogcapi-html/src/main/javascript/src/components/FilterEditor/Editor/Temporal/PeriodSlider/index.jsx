import React, { useState } from "react";
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

function SliderPeriod({ period, setPeriod, min, max, forStory }) {
  const [updated, setUpdated] = useState([period.start, moment(period.end).valueOf()]);

  const numSteps = 100;
  const range = max - min;
  const step = range / numSteps;

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
    setUpdated([new Date(updatedValues[0]), new Date(updatedValues[1])]);
    setPeriod((prevPeriod) => {
      return {
        ...prevPeriod,
        start: new Date(moment(updatedValues[0]).utc(true)),
        end: new Date(moment(updatedValues[1]).utc(true)),
      };
    });
  };

  // renderDateTime is only used for Storybook
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
      {forStory && renderDateTime(updated, "Date/Time")}
      <div style={{ margin: "1%", height: 120, width: "98%" }}>
        <Slider
          mode={1}
          step={step}
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
  min: PropTypes.number.isRequired,
  max: PropTypes.number.isRequired,
  setPeriod: PropTypes.func.isRequired,
  period: PropTypes.shape({
    start: PropTypes.instanceOf(Date).isRequired,
    end: PropTypes.instanceOf(Date),
  }).isRequired,
  forStory: PropTypes.bool,
};

SliderPeriod.defaultProps = {
  forStory: false,
};
