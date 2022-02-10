import React, { useCallback, useRef, useEffect } from "react";
import PropTypes from "prop-types";

import { RMap, RLayerTile, RLayerVectorTile, RControl } from "rlayers";
import { fromLonLat } from "ol/proj";
import { boundingExtent, getCenter } from "ol/extent";
import { MVT } from "ol/format";
import { setupProjections } from "./proj";
import DynamicView from "./DynamicView";
import DynamicSource from "./DynamicSource";

import "ol/ol.css";
import "./custom.css";

setupProjections();

const OpenLayers = ({
  backgroundUrl,
  bounds,
  attribution,
  dataUrl,
  tileMatrixSets,
}) => {
  const [currentFeature, setCurrentFeature] = React.useState(null);
  const [currentTileMatrixSet, setCurrentTileMatrixSet] = React.useState(
    tileMatrixSets[0] ? tileMatrixSets[0].tileMatrixSet : null
  );
  const prevTMSRef = useRef();
  useEffect(() => {
    prevTMSRef.current = currentTileMatrixSet;
  });
  const previousTileMatrixSet = prevTMSRef.current;

  // eslint-disable-next-line no-undef, no-underscore-dangle
  globalThis._map.setCurrentTileMatrixSet = setCurrentTileMatrixSet;

  const baseUrl = backgroundUrl.indexOf("{s}")
    ? backgroundUrl.replace(/\{s\}/, "{a-c}")
    : backgroundUrl;
  let initial = null;
  let extent = null;

  const tms = tileMatrixSets.find(
    (tms1) => tms1.tileMatrixSet === currentTileMatrixSet
  );

  if (tms) {
    initial = {
      center: fromLonLat([tms.defaultCenterLon, tms.defaultCenterLat]),
      zoom: tms.defaultZoomLevel,
    };
  } else if (bounds) {
    extent = boundingExtent([fromLonLat(bounds[0]), fromLonLat(bounds[1])]);
    initial = { center: getCenter(extent), zoom: 6 };
  }

  return (
    <>
      <RMap width="100%" height="100%" initial={initial} noDefaultControls>
        <DynamicView
          tileMatrixSet={tms}
          update={previousTileMatrixSet !== currentTileMatrixSet}
        />
        <RLayerTile
          properties={{ label: "Base map" }}
          url={baseUrl}
          attributions={attribution}
        />
        <RLayerVectorTile
          properties={{ label: "Vector tiles" }}
          url={dataUrl}
          format={new MVT()}
          onPointerEnter={useCallback(
            (e) => setCurrentFeature(e.target),
            [setCurrentFeature]
          )}
          onPointerLeave={useCallback(
            (e) => currentFeature === e.target && setCurrentFeature(null),
            [currentFeature, setCurrentFeature]
          )}
        >
          <DynamicSource
            tileMatrixSet={tms}
            dataUrl={dataUrl}
            update={previousTileMatrixSet !== currentTileMatrixSet}
          />
        </RLayerVectorTile>
        <RControl.RZoom />
        <RControl.RAttribution collapsible={false} />
      </RMap>
      <div id="map-info" style={{ opacity: currentFeature === null ? 0 : 1 }}>
        {currentFeature &&
          // eslint-disable-next-line no-underscore-dangle
          currentFeature.properties_ &&
          JSON.stringify(
            // eslint-disable-next-line no-underscore-dangle
            { id: currentFeature.id_, ...currentFeature.properties_ },
            null,
            2
          )}
      </div>
    </>
  );
};

OpenLayers.displayName = "OpenLayers";

OpenLayers.propTypes = {
  backgroundUrl: PropTypes.string,
  attribution: PropTypes.string,
  bounds: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
  dataUrl: PropTypes.string,
  tileMatrixSets: PropTypes.arrayOf(PropTypes.objectOf(PropTypes.string)),
};

OpenLayers.defaultProps = {
  backgroundUrl: "https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png",
  attribution:
    '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
  bounds: null,
  dataUrl: null,
  tileMatrixSets: [],
};

export default OpenLayers;
