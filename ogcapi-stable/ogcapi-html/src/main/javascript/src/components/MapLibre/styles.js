export const emptyStyle = () => ({
    version: 8,
    sources: {},
    layers: [],
});

export const baseStyle = (url, attribution, defaultUrl, defaultAttribution) => {
    const baseAttribution =
        url === defaultUrl && attribution !== defaultAttribution
            ? [attribution, defaultAttribution]
            : attribution;

    const servers =
        url.indexOf('{s}') > -1 || url.indexOf('{a-c}') > -1
            ? ['a', 'b', 'c'].map((prefix) =>
                  url.replace(/\{s\}/, prefix).replace(/\{a-c\}/, prefix)
              )
            : [url];

    return {
        version: 8,
        sources: {
            base: {
                type: 'raster',
                tiles: servers,
                tileSize: 256,
                attribution: baseAttribution,
            },
        },
        layers: [
            {
                id: 'background',
                type: 'raster',
                source: 'base',
            },
        ],
    };
};

export const hoverLayers = ['points', 'lines', 'polygons'];

const circleLayers = (color, opacity, circleRadius) => [
    {
        id: 'points',
        type: 'circle',
        source: 'data',
        paint: {
            'circle-color': color,
            'circle-opacity': opacity,
            'circle-radius': circleRadius,
        },
    },
];

const lineLayers = (color, opacity, lineWidth) => [
    {
        id: 'lines',
        type: 'line',
        source: 'data',
        layout: {
            'line-join': 'round',
            'line-cap': 'round',
        },
        paint: {
            'line-color': color,
            'line-opacity': opacity,
            'line-width': lineWidth,
        },
    },
];

const polygonLayers = (color, opacity, fillOpacity, outlineWidth) => [
    {
        id: 'polygons',
        type: 'fill',
        source: 'data',
        paint: {
            'fill-color': color,
            'fill-opacity': fillOpacity,
        },
    },
    {
        id: 'polygons-outline',
        type: 'line',
        source: 'data',
        layout: {
            'line-join': 'round',
            'line-cap': 'round',
        },
        paint: {
            'line-color': color,
            'line-opacity': opacity,
            'line-width': outlineWidth,
        },
    },
];

const withFilter = (layers, geometryType) =>
    layers.map((layer) => ({
        ...layer,
        filter: ['==', '$type', geometryType],
    }));

export const geoJsonLayers = ({
    color,
    opacity,
    circleRadius,
    lineWidth,
    fillOpacity,
    outlineWidth,
}) => {
    return ['Point', 'LineString', 'Polygon'].flatMap((geometryType) => {
        switch (geometryType) {
            case 'Point':
                return withFilter(
                    circleLayers(color, opacity, circleRadius),
                    geometryType
                );
            case 'LineString':
                return withFilter(
                    lineLayers(color, opacity, lineWidth),
                    geometryType
                );
            case 'Polygon':
                return withFilter(
                    polygonLayers(color, opacity, fillOpacity, outlineWidth),
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
        'source-layer': source,
        filter: ['==', '$type', geometryType],
   }));

export const vectorLayers = (
    source,
    geometryTypes,
    { color, opacity, circleRadius, lineWidth, fillOpacity, outlineWidth }
) =>
    geometryTypes.flatMap((geometryType) => {
        switch (geometryType) {
            case 'points':
                return withSourceAndFilter(
                    circleLayers(color, opacity, circleRadius),
                    source,
                    'Point'
                );
            case 'lines':
                return withSourceAndFilter(
                    lineLayers(color, opacity, lineWidth),
                    source,
                    'LineString'
                );
            case 'polygons':
                return withSourceAndFilter(
                    polygonLayers(color, opacity, fillOpacity, outlineWidth),
                    source,
                    'Polygon'
                );
            default:
                return [];
        }
    });
