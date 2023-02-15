import React, { useState, useEffect } from "react";
import FilterEditor from "..";

const baseUrl = "https://demo.ldproxy.net";

const FetchPropertiesEnum = ({ start, end, bounds }) => {
  const [fields, setFields] = useState({
    firstname: "Vorname",
    lastname: "Nachname",
  });
  const [titleForFilter, setTitleForFilter] = useState({
    firstname: "Vorname",
    lastname: "Nachname",
  });
  const [code, setCode] = useState("");
  const [relativeUrl, setRelativeUrl] = useState(
    "/strassen/collections/abschnitteaeste/queryables?f=json"
  );

  useEffect(() => {
    fetch(baseUrl + relativeUrl)
      .then((response) => {
        return response.json();
      })

      .then((obj) => {
        const streetProperties = {};
        for (const key in obj.properties) {
          if (obj.properties[key].title) {
            streetProperties[key] = obj.properties[key].title;
          }
        }
        setFields(streetProperties);
        setTitleForFilter(streetProperties);

        const streetCode = {};
        for (const key in obj.properties) {
          if (obj.properties[key].enum) {
            streetCode[key] = obj.properties[key].enum;
          }
        }
        setCode(streetCode);
      })

      .catch((error) => {
        console.error(error);
      });
  }, [relativeUrl]);

  return (
    <FilterEditor
      code={code}
      fields={fields}
      start={start}
      end={end}
      bounds={bounds}
      titleForFilter={titleForFilter}
    />
  );
};

export default FetchPropertiesEnum;
