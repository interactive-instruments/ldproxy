import React from "react";
import PropTypes from "prop-types";
import { RContext } from "rlayers";
import { View } from "ol";
import { fromLonLat } from "ol/proj";

const DynamicView = ({ tileMatrixSet, update }) => (
  <RContext.Consumer>
    {({ map }) => {
      if (update && tileMatrixSet) {
        map.setView(
          new View({
            center: fromLonLat(
              [tileMatrixSet.defaultCenterLon, tileMatrixSet.defaultCenterLat],
              tileMatrixSet.projection
            ),
            zoom: tileMatrixSet.defaultZoomLevel,
            projection: tileMatrixSet.projection,
          })
        );
      }
    }}
  </RContext.Consumer>
);
DynamicView.displayName = "DynamicView";

DynamicView.propTypes = {
  tileMatrixSet: PropTypes.objectOf(PropTypes.string),
  update: PropTypes.bool,
};

DynamicView.defaultProps = {
  tileMatrixSet: null,
  update: false,
};

export default DynamicView;
