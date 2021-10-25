// eslint-disable-next-line no-unused-vars
import React from 'react';
import PropTypes from 'prop-types';

import { useMaplibreUIEffect } from 'react-maplibre-ui';
import { geoJsonLayers, hoverLayers, vectorLayers } from '../styles';
import { getBounds, getFeaturesWithIdAsProperty, idProperty } from '../geojson';
import { addPopup, addPopupProps } from './popup';

const setStyleGeoJson = (map, styleUrl) => {
    const baseStyle = map.getStyle();
    // eslint-disable-next-line no-undef
    fetch(styleUrl)
        .then((response) => response.json())
        .then((style) => {
            const dataStyle = {
                ...style,
                sources: {
                    ...style.sources,
                    ...baseStyle.sources,
                },
                layers: style.layers.map((layer) => {
                    if (layer.type !== 'raster') {
                        const newLayer = {
                            ...layer,
                            source: 'data',
                        };
                        delete newLayer['source-layer'];
                        return newLayer;
                    }

                    return layer;
                }),
            };
            map.setStyle(dataStyle);
        });
};

const setStyleVector = (
    map,
    maplibre,
    styleUrl,
    popup,
    sourceUrl,
    sourceLayers
) => {
    const baseStyle = map.getStyle();
    // eslint-disable-next-line no-undef
    fetch(styleUrl)
        .then((response) => response.json())
        .then((style) => {
            let dataStyle = style;

            if (sourceUrl) {
                const newSources = {};
                Object.keys(style.sources).forEach((source) => {
                    if (style.sources[source].type === 'vector') {
                        newSources[source] = {
                            ...style.sources[source],
                            tiles: [sourceUrl],
                        };
                    } else {
                        newSources[source] = style.sources[source];
                    }
                });

                dataStyle = {
                    ...style,
                    sources: {
                        ...newSources,
                        ...baseStyle.sources,
                    },
                    layers: style.layers.filter(
                        (layer) =>
                            layer.type === 'raster' ||
                            (layer['source-layer'] &&
                                (sourceLayers.length === 0 ||
                                    sourceLayers.includes(
                                        layer['source-layer']
                                    )))
                    ),
                };
            }

            map.setStyle(dataStyle);

            if (popup === 'CLICK_PROPERTIES') {
                addPopupProps(
                    map,
                    maplibre,
                    dataStyle.layers
                        .filter((layer) => layer.type !== 'raster')
                        .map((layer) => layer.id)
                );
            }
        });
};

const addData = (
    map,
    maplibre,
    styleUrl,
    data,
    dataType,
    dataLayers,
    defaultStyle,
    fitBounds,
    popup
) => {
    if (dataType === 'geojson') {
        const features = getFeaturesWithIdAsProperty(data);

        map.addSource('data', {
            type: 'geojson',
            data: features,
            promoteId: idProperty,
        });

        if (fitBounds) {
            const bounds = getBounds(data);

            map.fitBounds(bounds, {
                padding: 50,
                maxZoom: 16,
                duration: 500,
            });
        }

        if (styleUrl) {
            setStyleGeoJson(map, styleUrl);
        } else {
            const defaultLayers = geoJsonLayers(defaultStyle);

            defaultLayers.forEach((layer) => map.addLayer(layer));

            if (popup === 'HOVER_ID') {
                addPopup(map, maplibre, hoverLayers);
            }
        }
    } else if (dataType === 'vector') {
        if (styleUrl) {
            setStyleVector(
                map,
                maplibre,
                styleUrl,
                popup,
                data,
                Object.keys(dataLayers)
            );
        } else {
            map.addSource('data', {
                type: 'vector',
                tiles: [data],
            });

            const layers = Object.keys(dataLayers).flatMap((layer) =>
                vectorLayers(layer, dataLayers[layer], defaultStyle)
            );

            layers.forEach((vectorLayer) => map.addLayer(vectorLayer));

            if (popup === 'CLICK_PROPERTIES') {
                addPopupProps(
                    map,
                    maplibre,
                    layers.map((l) => l.id)
                );
            }
        }
    }
};

const MapLibreConfiguration = ({
    styleUrl,
    data,
    dataType,
    dataLayers,
    controls,
    defaultStyle,
    fitBounds,
    popup,
    custom,
    showCompass,
}) => {
    useMaplibreUIEffect(({ map, maplibre }) => {
        map.addControl(
            new maplibre.AttributionControl({
                compact: false,
            })
        );
        map.addControl(new maplibre.ScaleControl());
        if (controls) {
            map.addControl(new maplibre.NavigationControl({ showCompass }));
        }
        if (data) {
            const style = {
                ...MapLibreConfiguration.defaultProps.defaultStyle,
                ...defaultStyle,
            };

            if (dataType === 'geojson' && typeof data === 'string') {
                // eslint-disable-next-line no-undef
                fetch(data)
                    .then((response) => response.json())
                    .then((json) => {
                        addData(
                            map,
                            maplibre,
                            styleUrl,
                            json,
                            dataType,
                            dataLayers,
                            style,
                            fitBounds,
                            popup
                        );
                    });
            } else {
                addData(
                    map,
                    maplibre,
                    styleUrl,
                    data,
                    dataType,
                    dataLayers,
                    style,
                    fitBounds,
                    popup
                );
            }
        } else if (styleUrl) {
            setStyleVector(map, maplibre, styleUrl, popup);
        }
        if (custom) {
            custom(map, maplibre);
        }
    }, []);

    return null;
};

MapLibreConfiguration.displayName = 'MapLibreConfiguration';

MapLibreConfiguration.propTypes = {
    styleUrl: PropTypes.string,
    data: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    dataType: PropTypes.string,
    dataLayers: PropTypes.objectOf(PropTypes.arrayOf(PropTypes.string)),
    controls: PropTypes.bool,
    defaultStyle: PropTypes.shape({
        color: PropTypes.string,
        opacity: PropTypes.number,
        circleRadius: PropTypes.number,
        lineWidth: PropTypes.number,
        fillOpacity: PropTypes.number,
        outlineWidth: PropTypes.number,
    }),
    fitBounds: PropTypes.bool,
    popup: PropTypes.oneOf(['HOVER_ID', 'CLICK_PROPERTIES']),
    custom: PropTypes.func,
    showCompass: PropTypes.bool,
};

MapLibreConfiguration.defaultProps = {
    styleUrl: null,
    data: null,
    dataType: 'geojson',
    dataLayers: {},
    controls: true,
    defaultStyle: {
        color: '#1D4E89',
        opacity: 1,
        circleRadius: 8,
        lineWidth: 4,
        fillOpacity: 0.2,
        outlineWidth: 2,
    },
    fitBounds: true,
    popup: null,
    custom: null,
    showCompass: true,
};

export default MapLibreConfiguration;
