import React from "react";
import PropTypes from "prop-types";
import { RContext } from "rlayers";
import { VectorTile as VectorTileSource } from "ol/source";
import TileGrid from "ol/tilegrid/TileGrid";
import { MVT } from "ol/format";

const DynamicView = ({ tileMatrixSet, dataUrl, update }) => (
  <RContext.Consumer>
    {({ layer }) => {
      if (update && tileMatrixSet) {
        layer.set(
          "source",
          new VectorTileSource({
            url: dataUrl.replace(
              "WebMercatorQuad",
              tileMatrixSet.tileMatrixSet
            ),
            format: new MVT(),
            maxZoom: tileMatrixSet.maxLevel,
            projection: tileMatrixSet.projection,
            tileGrid: new TileGrid({
              extent: JSON.parse(tileMatrixSet.extent),
              resolutions: JSON.parse(tileMatrixSet.resolutions),
              sizes: JSON.parse(tileMatrixSet.sizes),
            }),
          })
        );
      }
    }}
  </RContext.Consumer>
);
DynamicView.displayName = "DynamicView";

DynamicView.propTypes = {
  tileMatrixSet: PropTypes.objectOf(PropTypes.string),
  dataUrl: PropTypes.string,
  update: PropTypes.bool,
};

DynamicView.defaultProps = {
  tileMatrixSet: null,
  dataUrl: "",
  update: false,
};

export default DynamicView;
