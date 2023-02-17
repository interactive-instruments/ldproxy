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

const FetchSpatialTemporal = () => {
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");

  const [bounds, setBounds] = useState([]);
  // eslint-disable-next-line
  useEffect(() => {
    const url = new URL(
      baseUrl.pathname.endsWith("/") ? "../queryables" : "./queryables",
      baseUrl.href
    );

    if (!url.pathname.includes("queryables")) {
      return null;
    }

    url.search = "?f=json";

    fetch(url)
      .then((response) => {
        return response.json();
      })
      // eslint-disable-next-line
      .then((obj) => {
        if (!obj || !obj.extent) {
          return null;
        }
        const starting = obj.extent.temporal.interval[0];
        const ending = obj.extent.temporal.interval[1];

        if (!starting || !ending) {
          return null;
        }
        setStart(starting);
        setEnd(ending);

        const bound = obj.extent.spatial.bbox;
        if (!bound) {
          return null;
        }
        setBounds(bound);
      })

      .catch((error) => {
        return error;
      });
  }, []);

  return <FetchPropertiesEnum start={start} end={end} bounds={bounds} />;
};

export default FetchSpatialTemporal;
