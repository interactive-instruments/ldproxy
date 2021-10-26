import React from 'react';
import PropTypes from 'prop-types';

import MapLibre from '../../../../MapLibre';
import './style.css';

// eslint-disable-next-line no-unused-vars
const MapSelect = ({ bounds, backgroundUrl, attribution, onChange }) => {
    const cfg = (map, maplibre) => {
        const canvas = map.getCanvasContainer();

        // eslint-disable-next-line no-undef
        const box = document.createElement('div');
        box.classList.add('boxdraw');
        canvas.appendChild(box);

        const sw = map.project(bounds[0]);
        const ne = map.project(bounds[1]);
        const top = ne.y;
        const left = sw.x;
        const bottom = sw.y;
        const right = ne.x;

        box.style.top = `${top}px`;
        box.style.left = `${left}px`;
        box.style.width = `${right - left}px`;
        box.style.height = `${bottom - top}px`;

        map.on('idle', () => {
            const sw2 = map.unproject(new maplibre.Point(left, bottom));
            const ne2 = map.unproject(new maplibre.Point(right, top));

            onChange([
                [sw2.lng, sw2.lat],
                [ne2.lng, ne2.lat],
            ]);
        });
    };

    return (
        <MapLibre
            backgroundUrl={backgroundUrl}
            attribution={attribution}
            bounds={bounds}
            fitBoundsOptions={{ padding: 50, maxZoom: 16, animate: false }}
            showCompass={false}
            custom={cfg}
        />
    );
};

MapSelect.displayName = 'MapSelect';

MapSelect.propTypes = {
    bounds: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
    backgroundUrl: PropTypes.string,
    attribution: PropTypes.string,
    onChange: PropTypes.func.isRequired,
};

MapSelect.defaultProps = {
    bounds: [
        [0, 0],
        [0, 0],
    ],
    backgroundUrl: null,
    attribution: null,
};

export default MapSelect;
