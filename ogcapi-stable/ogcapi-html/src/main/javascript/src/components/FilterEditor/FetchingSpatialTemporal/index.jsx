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
  const [start, setStart] = useState();
  const [end, setEnd] = useState();
  const [temporal, setTemporal] = useState();
  const [spatial, setSpatial] = useState();
  const [dataFetched, setDataFetched] = useState(false);

  useEffect(() => {
    const url = new URL(baseUrl.pathname.endsWith("/") ? "../" : "./", baseUrl.href);

    url.search = "?f=json";

    fetch(url)
      .then((response) => response.json())
      .then((data) => {
        const { temporal: temporalExtent, spatial: spatialExtent } = data.extent;
        const temporalValues = parseTemporalExtent(temporalExtent);

        setTemporal(temporalValues);
        setStart(temporalValues.start);
        setEnd(temporalValues.end);

        const bounds = spatialExtent.bbox;
        const transformedBounds = bounds.map((innerArray) => [
          [innerArray[0], innerArray[1]],
          [innerArray[2], innerArray[3]],
        ]);
        const flattenedBounds = transformedBounds.flat();

        setSpatial(flattenedBounds);
        setDataFetched(true);
      })
      .catch((error) => console.log(error));
  }, []);

  if (!dataFetched) {
    return <div>Loading...</div>;
  }

  if (start === undefined || end === undefined) {
    return <FetchPropertiesEnum start={null} end={null} spatial={spatial} temporal={temporal} />;
  }

  if (temporal === undefined) {
    return <FetchPropertiesEnum start={start} end={end} spatial={spatial} temporal={null} />;
  }

  if (spatial === undefined || spatial === null) {
    return <FetchPropertiesEnum start={start} end={end} spatial={null} temporal={temporal} />;
  }

  return <FetchPropertiesEnum start={start} end={end} spatial={spatial} temporal={temporal} />;
};

export default FetchSpatialTemporal;
