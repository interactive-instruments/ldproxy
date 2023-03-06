import React, { useState, useEffect } from "react";
import { Slider, Rail, Handles, Tracks, Ticks } from "react-compound-slider";
import {
  subDays,
  startOfToday,
  addDays,
  format,
  differenceInMonths,
  differenceInHours,
} from "date-fns";
import { scaleTime } from "d3-scale";
import { SliderRail, Handle, Track, Tick } from "./components";

const halfHour = 1000 * 60 * 30;

const sliderStyle = {
  position: "relative",
  width: "100%",
};

const formatTick = (ms) => {
  return format(new Date(ms), "MMM dd");
};

function SliderFunction({ instant, isInstant, period, setInstant, setPeriod }) {
  const [updated, setUpdated] = useState(period.start, period.end);
  const min = period.start;
  const max = !isInstant ? addDays(period.end, 1) : startOfToday();
  console.log(updated);
  /*
  useEffect(() => {
    if (period.end) {
      setUpdated(period.start, period.end);
    }
  }, [period.start, period.end]);
'/
  /*
  const dayDifference = differenceInHours(max, min);
  max = !isInstant && dayDifference < 1 ? addDays(period.end, 1) : period.end;
  min = !isInstant && dayDifference < 1 ? subDays(period.start, 1) : period.start;
 */

  const dateTicks = scaleTime()
    .domain([min, max])
    .ticks(8)
    .map((d) => +d);

  const onUpdate = ([ms]) => {
    setUpdated(new Date(ms));
    setInstant(new Date(ms));
    // isInstant ? setInstant(new Date(ms)) : setPeriod(new Date(ms));
  };

  const renderDateTime = (date, header) => {
    const diffInMonths = differenceInMonths(max, min);
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
      {renderDateTime(updated, isInstant ? "Instant" : "Period")}
      <div style={{ margin: "5%", height: 120, width: "90%" }}>
        <Slider
          mode={1}
          step={halfHour}
          domain={[+min, +max]}
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
                    domain={[+min, +max]}
                    getHandleProps={getHandleProps}
                  />
                ))}
              </div>
            )}
          </Handles>
          <Tracks right={false}>
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

export default SliderFunction;
