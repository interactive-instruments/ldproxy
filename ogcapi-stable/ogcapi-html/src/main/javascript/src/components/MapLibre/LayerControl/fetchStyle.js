import React, { useEffect, useState } from "react";

export function useMapVisibility() {
  const [visibility, setVisibility] = useState(null);

  async function fetchJSONData(url) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      return await response.json();
    } catch (error) {
      console.error("Error fetching JSON data:", error);
      return null;
    }
  }

  const url = "https://demo.ldproxy.net/daraa/styles/topographic-with-basemap?f=mbs";

  useEffect(() => {
    fetchJSONData(url)
      .then((data) => {
        if (data) {
          const basemapLayer = data.layers.find((layer) => layer.id === "basemap");
          const visibility = basemapLayer.layout.visibility;

          console.log('Sichtbarkeit der Ebene "basemap":', visibility);
          if (visibility === "visible") {
            setVisibility(true);
          } else {
            setVisibility(false);
          }
        }
      })
      .catch((error) => {
        console.error("Error:", error);
      });
  }, []);
  return visibility;
}
