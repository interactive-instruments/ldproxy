import React from "react";
import { storiesOf } from "@storybook/react";
import SliderInstant from "./InstantSlider";
import SliderPeriod from "./PeriodSlider";

const Instant = (args) => {
  return (
    <>
      <SliderInstant {...args} />
    </>
  );
};

const Period = (args) => {
  return (
    <>
      <SliderPeriod {...args} />
    </>
  );
};

// Stories for SliderInstant:

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Instant/MoreThan7Years", module).add(
  "Default",
  () => (
    <Instant
      minInstant={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      maxInstant={new Date("Tue Jan 01 2028 00:57:00 GMT+0100")}
      setInstant={() => {}}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
      }}
      isInstant="true"
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Instant/MoreThan3Years", module).add(
  "Default",
  () => (
    <Instant
      minInstant={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      maxInstant={new Date("Tue Jan 01 2024 00:57:00 GMT+0100")}
      setInstant={() => {}}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
      }}
      isInstant="true"
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Instant/MoreThan7Months", module).add(
  "Default",
  () => (
    <Instant
      minInstant={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      maxInstant={new Date("Tue Dec 01 2019 00:57:00 GMT+0100")}
      setInstant={() => {}}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
      }}
      isInstant="true"
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Instant/MoreThan24h", module).add(
  "Default",
  () => (
    <Instant
      minInstant={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      maxInstant={new Date("Tue Jan 08 2019 00:00:00 GMT+0100")}
      setInstant={() => {}}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
      }}
      isInstant="true"
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Instant/LessThan24h", module).add(
  "Default",
  () => (
    <Instant
      minInstant={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      maxInstant={new Date("Tue Jan 01 2019 23:00:00 GMT+0100")}
      setInstant={() => {}}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
      }}
      isInstant="true"
      forStory
    />
  )
);

// Stories for SliderPeriod:

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Period/MoreThan7Years", module).add(
  "Default",
  () => (
    <Period
      isInstant={false}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
        end: new Date("Tue Dec 31 2028 00:00:00 GMT+0100"),
      }}
      setPeriod={() => {}}
      min={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      max={new Date("Tue Dec 31 2028 00:00:00 GMT+0100")}
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Period/MoreThan3Years", module).add(
  "Default",
  () => (
    <Period
      isInstant={false}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
        end: new Date("Tue Jan 01 2024 00:00:00 GMT+0100"),
      }}
      setPeriod={() => {}}
      min={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      max={new Date("Tue Jan 01 2024 00:00:00 GMT+0100")}
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Period/MoreThan7Months", module).add(
  "Default",
  () => (
    <Period
      isInstant={false}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
        end: new Date("Tue Dec 01 2019 00:00:00 GMT+0100"),
      }}
      setPeriod={() => {}}
      min={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      max={new Date("Tue Dec 01 2019 00:00:00 GMT+0100")}
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Period/MoreThan24h", module).add(
  "Default",
  () => (
    <Period
      isInstant={false}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
        end: new Date("Tue Jun 07 2019 00:00:00 GMT+0100"),
      }}
      setPeriod={() => {}}
      min={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      max={new Date("Tue Jun 07 2019 00:00:00 GMT+0100")}
      forStory
    />
  )
);

storiesOf("@ogcapi/html/FilterEditor/Editor/Temporal/Period/LessThan24h", module).add(
  "Default",
  () => (
    <Period
      isInstant={false}
      period={{
        start: new Date("Tue Jan 01 2019 00:00:00 GMT+0100"),
        end: new Date("Tue Jan 01 2019 23:00:00 GMT+0100"),
      }}
      setPeriod={() => {}}
      min={new Date("Tue Jan 01 2019 00:00:00 GMT+0100")}
      max={new Date("Tue Jan 01 2019 23:00:00 GMT+0100")}
      forStory
    />
  )
);
