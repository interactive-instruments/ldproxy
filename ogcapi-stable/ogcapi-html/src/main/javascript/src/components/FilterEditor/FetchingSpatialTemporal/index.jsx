import React, { useEffect, useState } from "react";
import FetchPropertiesEnum from "../FetchingPropertiesEnum";

let baseUrl = new URL(window.location.href);
if (process.env.NODE_ENV !== "production") {
  console.log("DEV");
  baseUrl = new URL(
    "https://demo.ldproxy.net/strassen/collections/abschnitteaeste/items?limit=10&offset=10"
  );
  // slash at the end should also work
  /* baseUrl = new URL(
    "https://demo.ldproxy.net/strassen/collections/abschnitteaeste/items/?limit=10&offset=10"
  ); */
}

const parseTemporalExtent = (temporalExtent) => {
  const starting = temporalExtent.interval[0][0];
  const ending = temporalExtent.interval[0][1];
  const startingUnix = new Date(starting).getTime();
  const endingUnix = new Date(ending).getTime();

  return { start: startingUnix, end: endingUnix };
};

const FetchSpatialTemporal = () => {
  const [start, setStart] = useState(10);
  const [end, setEnd] = useState(10);
  const [temporal, setTemporal] = useState({});
  const [spatial, setSpatial] = useState([]);
  const [dataFetched, setDataFetched] = useState(false);

  useEffect(() => {
    const url = new URL(baseUrl.pathname.endsWith("/") ? "../" : "./", baseUrl.href);

    url.search = "?f=json";

    fetch(url)
      .then((response) => response.json())
      .then((data) => {
        const { temporal: temporalExtent, spatial: spatialExtent } = data.extent;

        let temporalValues = {};

        if (temporalExtent) {
          temporalValues = parseTemporalExtent(temporalExtent);
        }

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
        const hasTemporal = Object.keys(temporalValues).length > 0;

        if (hasSpatial && hasTemporal) {
          setTemporal(temporalValues);
          setStart(temporalValues.start);
          setEnd(temporalValues.end);
          setSpatial(flattenedBounds);
          setDataFetched(true);
        } else if (!hasSpatial) {
          setSpatial([]);
          setTemporal(temporalValues);
          setStart(temporalValues.start);
          setEnd(temporalValues.end);
          setDataFetched(true);
        } else if (!hasTemporal) {
          setSpatial(flattenedBounds);
          setTemporal({});
          setStart(null);
          setEnd(null);
          setDataFetched(true);
        }
      })
      .catch((error) => console.log(error));
  }, []);

  if (!dataFetched) {
    return <div>Loading...</div>;
  }

  return <FetchPropertiesEnum start={start} end={end} spatial={spatial} temporal={temporal} />;
};

export default FetchSpatialTemporal;
