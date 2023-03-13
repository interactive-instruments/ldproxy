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

function SliderInstant({ minInstant, maxInstant, period, setInstant, forStory }) {
  const [updated, setUpdated] = useState(period.start);

  const numSteps = 100;
  const range = maxInstant - minInstant;
  const step = range / numSteps;

  const formatTick = (ms) => {
    let dateFormat;
    if (differenceInYears(maxInstant, minInstant) > 7) {
      dateFormat = format(new Date(ms), "yyyy");
    } else if (differenceInYears(maxInstant, minInstant) > 3) {
      dateFormat = format(new Date(ms), "MMM yyyy");
    } else if (differenceInMonths(maxInstant, minInstant) > 7) {
      dateFormat = format(new Date(ms), "MMM");
    } else if (differenceInHours(maxInstant, minInstant) > 24) {
      dateFormat = format(new Date(ms), "MMM dd");
    } else if (differenceInHours(maxInstant, minInstant) < 24) {
      dateFormat = format(new Date(ms), "HH:mm:ss");
    }
    return dateFormat;
  };

  const dateTicks = scaleTime()
    .domain([minInstant, maxInstant])
    .ticks(8)
    .map((d) => +d);

  const onUpdate = ([ms]) => {
    setUpdated(new Date(ms));
    setInstant(moment(ms).utc(true));
  };

  // renderDateTime is only used for Storybook
  const renderDateTime = (date, header) => {
    const diffInMonths = differenceInMonths(maxInstant, minInstant);
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

  return (
    <div>
      {forStory && renderDateTime(updated, "Date/Time")}
      <div style={{ margin: "1%", height: 120, width: "98%" }}>
        <Slider
          mode={1}
          step={step}
          domain={[+minInstant, +maxInstant]}
          rootStyle={sliderStyle}
          onUpdate={onUpdate}
          values={[+updated]}
        >
          <Rail>{({ getRailProps }) => <SliderRail getRailProps={getRailProps} />}</Rail>
          <Handles>
            {({ handles, getHandleProps }) => (
              <div>
                {handles.map((handle) => (
                  <Handle
                    key={handle.id}
                    handle={handle}
                    domain={[+minInstant, +maxInstant]}
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

SliderInstant.propTypes = {
  minInstant: PropTypes.number.isRequired,
  maxInstant: PropTypes.number.isRequired,
  setInstant: PropTypes.func.isRequired,
  period: PropTypes.shape({
    start: PropTypes.instanceOf(Date).isRequired,
    end: PropTypes.instanceOf(Date),
  }).isRequired,
  forStory: PropTypes.bool,
};

SliderInstant.defaultProps = {
  forStory: false,
};

export default SliderInstant;
