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
      .then((obj) => {
        if (!obj || !obj.properties) {
          return null;
        }
        const streetProperties = {};
        // eslint-disable-next-line no-restricted-syntax
        for (const key in obj.properties) {
          if (obj.properties[key].title) {
            streetProperties[key] = obj.properties[key].title;
          }
        }

        if (Object.keys(streetProperties).length === 0) {
          return null;
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

        if (Object.keys(streetCode).length === 0) {
          return null;
        }

        setCode(streetCode);
      })

      .catch((error) => {
        return error;
      });
  }, []);

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
