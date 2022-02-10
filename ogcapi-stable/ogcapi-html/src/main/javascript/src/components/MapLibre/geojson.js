import geoJsonExtent from '@mapbox/geojson-extent';

export const getBounds = geoJsonExtent;

export const idProperty = '__id';

export const isCollection = (geojson) => geojson.type === 'FeatureCollection';

const featureWithIdAsProperty = (feature) => ({
    ...feature,
    properties: {
        ...feature.properties,
        [idProperty]: feature.id,
    },
});

export const getFeaturesWithIdAsProperty = (geojson) =>
    isCollection(geojson)
        ? {
              ...geojson,
              features: geojson.features.map(featureWithIdAsProperty),
          }
        : featureWithIdAsProperty(geojson);

export const polygonFromBounds = (bounds) => ({
    type: 'Feature',
    geometry: {
        type: 'Polygon',
        coordinates: [
            [
                [bounds[0][0], bounds[0][1]],
                [bounds[1][0], bounds[0][1]],
                [bounds[1][0], bounds[1][1]],
                [bounds[0][0], bounds[1][1]],
                [bounds[0][0], bounds[0][1]],
            ],
        ],
    },
    properties: {},
});
