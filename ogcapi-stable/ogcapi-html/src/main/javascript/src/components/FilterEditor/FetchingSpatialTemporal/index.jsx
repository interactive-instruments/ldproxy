import React, { useEffect, useState } from "react";
import FetchPropertiesEnum from "../FetchingPropertiesEnum";

const FetchSpatialTemporal = () => {
  const baseUrl = "https://demo.ldproxy.net";
  const [relativeUrl] = useState("/strassen/collections/abschnitteaeste?f=json");

  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");

  const [bounds, setBounds] = useState([]);

  useEffect(() => {
    fetch(baseUrl + relativeUrl)
      .then((response) => {
        return response.json();
      })

      .then((obj) => {
        const starting = obj.extent.temporal.interval[0];
        const ending = obj.extent.temporal.interval[1];
        setStart(starting);
        setEnd(ending);

        const bound = obj.extent.spatial.bbox;
        setBounds(bound);
      })

      .catch((error) => {
        return error;
      });
  }, [relativeUrl]);

  return <FetchPropertiesEnum start={start} end={end} bounds={bounds} />;
};

export default FetchSpatialTemporal;
