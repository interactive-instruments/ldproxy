/*import React, { useState, useEffect } from "react";
import { useApiInfo } from "./hooks";
import FilterEditor from "..";

export const extractFields = ({ baseUrl }) => {
  const [fields, setFields] = useState({
    firstname: "Vorname",
    lastname: "Nachname",
  });
  const [titleForFilter, setTitleForFilter] = useState({
    firstname: "Vorname",
    lastname: "Nachname",
  });
  const [code, setCode] = useState({});
  const [dataFetched, setDataFetched] = useState(false);

  const url = new URL(
    baseUrl.pathname.endsWith("/") ? "../queryables" : "./queryables",
    baseUrl.href
  );
  url.search = "?f=json";

  const { obj, isLoaded, error } = useApiInfo(url);

  useEffect(() => {
    if (isLoaded) {
      const streetProperties = {};
      const streetCode = {};

      // eslint-disable-next-line
      for (const key in obj.properties) {
        if (obj.properties[key].title) {
          streetProperties[key] = obj.properties[key].title;
        }
        if (obj.properties[key].enum) {
          streetCode[key] = obj.properties[key].enum;
        }
      }

      const hasTitles = Object.keys(streetProperties).length > 0;
      const hasEnums = Object.keys(streetCode).length > 0;

      if (hasTitles && hasEnums) {
        setFields(streetProperties);
        setTitleForFilter(streetProperties);
        setCode(streetCode);
        setDataFetched(true);
      } else if (!hasTitles) {
        setFields({});
        setTitleForFilter({});
        setCode(streetCode);
        setDataFetched(true);
      } else if (!hasEnums) {
        setFields(streetProperties);
        setTitleForFilter(streetProperties);
        setCode({});
        setDataFetched(true);
      }
    } else if (error) {
      return <div>Error: {error.message}</div>;
    }
  }, []);

  if (!dataFetched) {
    return <div>Loading...</div>;
  }

  return <FilterEditor code={code} fields={fields} titleForFilter={titleForFilter} />;
};

export const extractInterval = ({ baseUrl }) => {
  const [start, setStart] = useState(10);
  const [end, setEnd] = useState(10);
  const [temporal, setTemporal] = useState({});
  const [dataFetched, setDataFetched] = useState(false);

  const url = new URL(baseUrl.pathname.endsWith("/") ? "../" : "./", baseUrl.href);
  url.search = "?f=json";

  const { obj, isLoaded, error } = useApiInfo(url);

  const parseTemporalExtent = (temporalExtent) => {
    const starting = temporalExtent.interval[0][0];
    const ending = temporalExtent.interval[0][1];
    const startingUnix = new Date(starting).getTime();
    const endingUnix = new Date(ending).getTime();

    return { start: startingUnix, end: endingUnix };
  };

  useEffect(() => {
    if (isLoaded) {
      const { temporal: temporalExtent } = obj.extent;

      let temporalValues = {};

      if (temporalExtent) {
        temporalValues = parseTemporalExtent(temporalExtent);
      }

      const hasTemporal = Object.keys(temporalValues).length > 0;

      if (hasTemporal) {
        setTemporal(temporalValues);
        setStart(temporalValues.start);
        setEnd(temporalValues.end);
        setDataFetched(true);
      } else if (!hasTemporal) {
        setTemporal({});
        setStart(null);
        setEnd(null);
        setDataFetched(true);
      }
    } else if (error) {
      return <div>Error: {error.message}</div>;
    }
  }, []);

  if (!dataFetched) {
    return <div>Loading...</div>;
  }

  return <FilterEditor start={start} end={end} temporal={temporal} />;
};

export const spatial = ({ baseUrl }) => {
  const [spatial, setSpatial] = useState([]);
  const [dataFetched, setDataFetched] = useState(false);

  const url = new URL(baseUrl.pathname.endsWith("/") ? "../" : "./", baseUrl.href);
  url.search = "?f=json";

  const { obj, isLoaded, error } = useApiInfo(url);

  useEffect(() => {
    if (isLoaded) {
      const { spatial: spatialExtent } = obj.extent;

      let flattenedBounds = [];

      if (spatialExtent) {
        const bounds = spatialExtent.bbox;
        const transformedBounds = bounds.map((innerArray) => [
          [innerArray[0], innerArray[1]],
          [innerArray[2], innerArray[3]],
        ]);
        flattenedBounds = transformedBounds.flat();
      }

      const hasSpatial = flattenedBounds.length > 0;

      if (hasSpatial) {
        setSpatial(flattenedBounds);
        setDataFetched(true);
      } else if (!hasSpatial) {
        setSpatial([]);
        setDataFetched(true);
      }
    } else if (error) {
      return <div>Error: {error.message}</div>;
    }
  }, []);

  if (!dataFetched) {
    return <div>Loading...</div>;
  }

  return <FilterEditor spatial={spatial} />;
};
*/
