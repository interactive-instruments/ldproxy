import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import FilterEditor from "..";

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

const FetchPropertiesEnum = ({ start, end, bounds }) => {
  const [fields, setFields] = useState({
    firstname: "Vorname",
    lastname: "Nachname",
  });
  const [titleForFilter, setTitleForFilter] = useState({
    firstname: "Vorname",
    lastname: "Nachname",
  });
  const [code, setCode] = useState({});
  const [relativeUrl] = useState("/strassen/collections/abschnitteaeste/queryables?f=json");

  useEffect(() => {
    const url = new URL(
      baseUrl.pathname.endsWith("/") ? "../queryables" : "./queryables",
      baseUrl.href
    );
    url.search = "?f=json";

    fetch(url)
      .then((response) => {
        return response.json();
      })

      .then((obj) => {
        const streetProperties = {};
        // eslint-disable-next-line no-restricted-syntax
        for (const key in obj.properties) {
          if (obj.properties[key].title) {
            streetProperties[key] = obj.properties[key].title;
          }
        }
        setFields(streetProperties);
        setTitleForFilter(streetProperties);

        const streetCode = {};
        // eslint-disable-next-line no-restricted-syntax
        for (const key in obj.properties) {
          if (obj.properties[key].enum) {
            streetCode[key] = obj.properties[key].enum;
          }
        }
        setCode(streetCode);
      })

      .catch((error) => {
        return error;
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

FetchPropertiesEnum.propTypes = {
  start: PropTypes.string.isRequired,
  end: PropTypes.string.isRequired,
  bounds: PropTypes.arrayOf(PropTypes.number).isRequired,
};
