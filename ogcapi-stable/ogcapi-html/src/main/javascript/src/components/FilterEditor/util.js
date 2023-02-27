export const getBaseUrl = () => {
  let baseUrl = new URL(window.location.href);
  if (process.env.NODE_ENV !== "production") {
    console.log("DEV");
    baseUrl = new URL(
      "https://demo.ldproxy.net/strassen/collections/nullpunkte/items?limit=10&offset=10"
    );
    // slash at the end should also work
    /* baseUrl = new URL(
  "https://demo.ldproxy.net/strassen/collections/abschnitteaeste/items/?limit=10&offset=10"
  ); */
  }
  return baseUrl;
};

export const extractFields = (obj) => {
  const fields = {};
  const code = {};
  const integerKeys = [];
  const booleanProperty = [];
  if (obj && obj.properties) {
    // eslint-disable-next-line
    for (const key in obj.properties) {
      if (obj.properties[key].title) {
        fields[key] = obj.properties[key].title;
      }
      if (obj.properties[key].enum) {
        code[key] = obj.properties[key].enum;
      }
      if (obj.properties[key].type === "integer") {
        integerKeys.push(key);
      }
      if (obj.properties[key].type === "boolean") {
        booleanProperty.push(key);
      }
    }
  }

  return { fields, code, integerKeys, booleanProperty };
};

export const extractInterval = (obj) => {
  let start = null;
  let end = null;
  let temporal = {};

  const parseTemporalExtent = (temporalExtent) => {
    const starting = temporalExtent.interval[0][0];
    const ending = temporalExtent.interval[0][1];
    const startingUnix = new Date(starting).getTime();
    const endingUnix = new Date(ending).getTime();

    return { start: startingUnix, end: endingUnix };
  };

  if (obj && obj.extent) {
    const { temporal: temporalExtent } = obj.extent;

    if (temporalExtent) {
      temporal = parseTemporalExtent(temporalExtent);
    }

    start = temporal.start;
    end = temporal.end;
  }

  return { start, end, temporal };
};

export const extractSpatial = (obj) => {
  let spatial = [];

  if (obj && obj.extent) {
    const { spatial: spatialExtent } = obj.extent;

    if (spatialExtent) {
      const bounds = spatialExtent.bbox;
      const transformedBounds = bounds.map((innerArray) => [
        [innerArray[0], innerArray[1]],
        [innerArray[2], innerArray[3]],
      ]);
      spatial = transformedBounds.flat();
    }
  }
  return { spatial };
};
