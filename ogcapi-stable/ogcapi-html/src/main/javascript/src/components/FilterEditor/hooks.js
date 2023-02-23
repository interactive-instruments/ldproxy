import { useEffect, useState } from "react";

export const useApiInfo = (relativeUrl) => {
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

  // TODO: custom hook that encapsulates fetch logic from FetchingPropertiesEnum + FetchingSpatialTemporal
  // return the plain json object, extraction of fields/codes/bounds etc. goes to util.js

  const [obj, setObj] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState();

  useEffect(() => {
    fetch(relativeUrl)
      .then((response) => response.json())
      .then((data) => {
        setObj(data);
        setIsLoaded(true);
      })
      .catch((errors) => setError(errors));
  }, [relativeUrl]);

  return { baseUrl, obj, isLoaded, error };
};
