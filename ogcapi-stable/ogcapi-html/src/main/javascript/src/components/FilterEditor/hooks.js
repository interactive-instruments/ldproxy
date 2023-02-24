import { useEffect, useState } from "react";

export const useApiInfo = (url) => {
  // TODO: custom hook that encapsulates fetch logic from FetchingPropertiesEnum + FetchingSpatialTemporal
  // return the plain json object, extraction of fields/codes/bounds etc. goes to util.js

  const [obj, setObj] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(url)
      .then((response) => response.json())
      .then((data) => {
        setObj(data);
        setIsLoaded(true);
      })
      .catch((errors) => setError(errors));
  }, []);

  return { obj, isLoaded, error };
};
