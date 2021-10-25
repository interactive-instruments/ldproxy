import React from 'react';
import PropTypes from 'prop-types';

import { Map } from 'react-maplibre-ui';
import 'maplibre-gl/dist/maplibre-gl.css';
import './custom.css';

import Configuration from './Configuration';
import { baseStyle, emptyStyle } from './styles';
import { polygonFromBounds } from './geojson';

export { polygonFromBounds };

const MapLibre = ({
    styleUrl,
    backgroundUrl,
    center,
    zoom,
    bounds,
    attribution,
    dataUrl,
    dataType,
    dataLayers,
    interactive,
    savePosition,
    drawBounds,
    defaultStyle,
    fitBoundsOptions,
    popup,
    custom,
    showCompass,
}) => {
    const style = styleUrl
        ? emptyStyle()
        : baseStyle(
              backgroundUrl,
              attribution,
              MapLibre.defaultProps.backgroundUrl,
              MapLibre.defaultProps.attribution
          );

    const data = drawBounds && bounds ? polygonFromBounds(bounds) : dataUrl;

    return (
        <Map
            mapStyle={style}
            defaultCenter={center}
            defaultZoom={zoom}
            style={{
                height: '100%',
                width: '100%',
            }}
            customParameters={{
                bounds,
                fitBoundsOptions,
                attributionControl: false,
                interactive,
                hash: savePosition ? 'position' : false,
            }}>
            <Configuration
                styleUrl={styleUrl}
                data={data}
                dataType={dataType}
                dataLayers={dataLayers}
                controls={interactive}
                showCompass={showCompass}
                defaultStyle={defaultStyle}
                fitBounds={!drawBounds}
                popup={popup}
                custom={custom}
            />
        </Map>
    );
};

MapLibre.displayName = 'MapLibre';

MapLibre.propTypes = {
    styleUrl: PropTypes.string,
    backgroundUrl: PropTypes.string,
    attribution: PropTypes.string,
    center: PropTypes.arrayOf(PropTypes.number),
    zoom: PropTypes.number,
    bounds: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
    interactive: PropTypes.bool,
    savePosition: PropTypes.bool,
    dataUrl: PropTypes.string,
    drawBounds: PropTypes.bool,
    // eslint-disable-next-line react/forbid-prop-types
    defaultStyle: PropTypes.object,
    // eslint-disable-next-line react/forbid-prop-types
    fitBoundsOptions: PropTypes.object,
    ...Configuration.propTypes,
};

MapLibre.defaultProps = {
    styleUrl: null,
    backgroundUrl: 'https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution:
        '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
    center: [0, 0],
    zoom: 0,
    bounds: null,
    interactive: true,
    savePosition: false,
    drawBounds: false,
    dataUrl: null,
    defaultStyle: undefined,
    fitBoundsOptions: {
        padding: 30,
        maxZoom: 16,
        animate: false,
    },
    ...Configuration.defaultProps,
};

export default MapLibre;
