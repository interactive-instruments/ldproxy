import React, { useEffect, useState } from "react";
import FetchPropertiesEnum from "../FetchingPropertiesEnum";

const FetchSpatialTemporal = () => {
  const baseUrl = "https://demo.ldproxy.net";
  const [relativeUrl, setRelativeUrl] = useState("/strassen/collections/abschnitteaeste?f=json");

  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");

  const [bounds, setBounds] = useState([]);

  useEffect(() => {
    fetch(baseUrl + relativeUrl)
      .then((response) => {
        return response.json();
      })

      .then((obj) => {
        const start = obj.extent.temporal.interval[0];
        const end = obj.extent.temporal.interval[1];
        setStart(start);
        setEnd(end);

        const bounds = obj.extent.spatial.bbox;
        setBounds(bounds);
      })

      .catch((error) => {
        console.error(error);
      });
  }, [relativeUrl]);

  return <FetchPropertiesEnum start={start} end={end} bounds={bounds} />;
};

export default FetchSpatialTemporal;
