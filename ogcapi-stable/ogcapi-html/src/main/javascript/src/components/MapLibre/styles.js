export const emptyStyle = () => ({
  version: 8,
  sources: {},
  layers: [],
});

export const baseStyle = (url, attribution, defaultUrl, defaultAttribution) => {
  const baseAttribution =
    url === defaultUrl && attribution !== defaultAttribution
      ? [attribution, defaultAttribution]
      : attribution || defaultAttribution;

  const finalUrl = url || defaultUrl;

  const servers =
    finalUrl.indexOf("{s}") > -1 || finalUrl.indexOf("{a-c}") > -1
      ? ["a", "b", "c"].map((prefix) =>
          finalUrl.replace(/\{s\}/, prefix).replace(/\{a-c\}/, prefix)
        )
      : [finalUrl];

  return {
    version: 8,
    sources: {
      base: {
        type: "raster",
        tiles: servers,
        tileSize: 256,
        attribution: baseAttribution,
      },
    },
    layers: [
      {
        id: "background",
        type: "raster",
        source: "base",
      },
    ],
  };
};

export const hoverLayers = ["points", "lines", "polygons"];

export const isDataLayer = (layer) => {
  switch (layer.type) {
    case "raster":
    case "hillshade":
    case "background":
      return false;
    default:
      return true;
  }
};

const circleLayers = (color, opacity, circleRadius, minZoom, maxZoom) => [
  {
    id: "points",
    type: "circle",
    source: "data",
    paint: {
      "circle-color": color,
      "circle-opacity": opacity,
      "circle-radius": circleRadius,
    },
    minzoom: minZoom,
    maxzoom: maxZoom,
  },
];

const lineLayers = (color, opacity, lineWidth, minZoom, maxZoom) => [
  {
    id: "lines",
    type: "line",
    source: "data",
    layout: {
      "line-join": "round",
      "line-cap": "round",
    },
    paint: {
      "line-color": color,
      "line-opacity": opacity,
      "line-width": lineWidth,
    },
    minzoom: minZoom,
    maxzoom: maxZoom,
  },
];

const polygonLayers = (
  color,
  opacity,
  fillOpacity,
  outlineWidth,
  minZoom,
  maxZoom
) => [
  {
    id: "polygons",
    type: "fill",
    source: "data",
    paint: {
      "fill-color": color,
      "fill-opacity": fillOpacity,
    },
    minzoom: minZoom,
    maxzoom: maxZoom,
  },
  {
    id: "polygons-outline",
    type: "line",
    source: "data",
    layout: {
      "line-join": "round",
      "line-cap": "round",
    },
    paint: {
      "line-color": color,
      "line-opacity": opacity,
      "line-width": outlineWidth,
    },
    minzoom: minZoom,
    maxzoom: maxZoom,
  },
];

const withFilter = (layers, geometryType) =>
  layers.map((layer) => ({
    ...layer,
    filter: ["==", "$type", geometryType],
  }));

export const geoJsonLayers = ({
  color,
  opacity,
  circleRadius,
  circleMinZoom,
  circleMaxZoom,
  lineWidth,
  lineMinZoom,
  lineMaxZoom,
  fillOpacity,
  outlineWidth,
  polygonMinZoom,
  polygonMaxZoom,
}) => {
  return ["Polygon", "LineString", "Point"].flatMap((geometryType) => {
    switch (geometryType) {
      case "Point":
        return withFilter(
          circleLayers(
            color,
            opacity,
            circleRadius,
            circleMinZoom,
            circleMaxZoom
          ),
          geometryType
        );
      case "LineString":
        return withFilter(
          lineLayers(color, opacity, lineWidth, lineMinZoom, lineMaxZoom),
          geometryType
        );
      case "Polygon":
        return withFilter(
          polygonLayers(
            color,
            opacity,
            fillOpacity,
            outlineWidth,
            polygonMinZoom,
            polygonMaxZoom
          ),
          geometryType
        );
      default:
        return [];
    }
  });
};

const withSourceAndFilter = (layers, source, geometryType) =>
  layers.map((layer) => ({
    ...layer,
    id: `${source}_${layer.id}`,
    "source-layer": source,
    filter: ["==", "$type", geometryType],
  }));

export const vectorLayers = (
  source,
  geometryTypes,
  {
    color,
    opacity,
    circleRadius,
    circleMinZoom,
    circleMaxZoom,
    lineWidth,
    lineMinZoom,
    lineMaxZoom,
    fillOpacity,
    outlineWidth,
    polygonMinZoom,
    polygonMaxZoom,
  }
) =>
  geometryTypes.flatMap((geometryType) => {
    switch (geometryType) {
      case "points":
        return withSourceAndFilter(
          circleLayers(
            color,
            opacity,
            circleRadius,
            circleMinZoom,
            circleMaxZoom
          ),
          source,
          "Point"
        );
      case "lines":
        return withSourceAndFilter(
          lineLayers(color, opacity, lineWidth, lineMinZoom, lineMaxZoom),
          source,
          "LineString"
        );
      case "polygons":
        return withSourceAndFilter(
          polygonLayers(
            color,
            opacity,
            fillOpacity,
            outlineWidth,
            polygonMinZoom,
            polygonMaxZoom
          ),
          source,
          "Polygon"
        );
      default:
        return [];
    }
  });
