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

const FetchPropertiesEnum = ({ start, end, spatial, temporal }) => {
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
        if (!obj || !obj.properties) {
          return null;
        }
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
      })
      .catch((error) => {
        return error;
      });
  }, []);

  if (!dataFetched) {
    return <div>Loading...</div>;
  }

  return (
    <FilterEditor
      code={code}
      fields={fields}
      start={start}
      end={end}
      spatial={spatial}
      titleForFilter={titleForFilter}
      temporal={temporal}
    />
  );
};

export default FetchPropertiesEnum;

FetchPropertiesEnum.propTypes = {
  start: PropTypes.number.isRequired,
  end: PropTypes.number.isRequired,
  spatial: PropTypes.arrayOf(PropTypes.number).isRequired,
  temporal: PropTypes.objectOf(PropTypes.number).isRequired,
};
