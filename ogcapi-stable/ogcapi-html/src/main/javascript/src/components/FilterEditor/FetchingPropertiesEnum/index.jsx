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

  const [integerKeys, setIntegerKeys] = useState([]);

  // eslint-disable-next-line
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
      // eslint-disable-next-line
      .then((obj) => {
        if (!obj || !obj.properties) {
          return null;
        }

        const streetProperties = {};
        const streetCode = {};
        const types = [];

        // eslint-disable-next-line
        for (const key in obj.properties) {
          if (obj.properties[key].title) {
            streetProperties[key] = obj.properties[key].title;
          }
          if (obj.properties[key].enum) {
            streetCode[key] = obj.properties[key].enum;
          }
          if (obj.properties[key].type === "integer") {
            types.push(key);
          }
        }

        const hasTitles = Object.keys(streetProperties).length > 0;
        const hasEnums = Object.keys(streetCode).length > 0;
        const hasTypes = types.length > 0;

        switch (true) {
          case hasTitles && hasEnums && hasTypes:
            setFields(streetProperties);
            setTitleForFilter(streetProperties);
            setCode(streetCode);
            setIntegerKeys(types);
            setDataFetched(true);
            break;
          case hasTitles && hasEnums:
            setFields(streetProperties);
            setTitleForFilter(streetProperties);
            setCode(streetCode);
            setIntegerKeys([]);
            setDataFetched(true);
            break;
          case hasTitles && hasTypes:
            setFields(streetProperties);
            setTitleForFilter(streetProperties);
            setCode({});
            setIntegerKeys(types);
            setDataFetched(true);
            break;
          case hasTitles:
            setFields(streetProperties);
            setTitleForFilter(streetProperties);
            setCode({});
            setIntegerKeys([]);
            setDataFetched(true);
            break;
          case !hasEnums && !hasTitles && !hasTypes:
            setFields({});
            setTitleForFilter({});
            setCode({});
            setIntegerKeys([]);
            setDataFetched(true);
            break;
          default:
            setDataFetched(false);
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
      integerKeys={integerKeys}
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
