import {
  Viewer,
  Ion,
  Camera,
  Credit,
  WebMapTileServiceImageryProvider,
  Rectangle,
  CesiumTerrainProvider,
  IonResource,
  Cesium3DTileset,
  Cesium3DTileStyle,
  Matrix4,
  Cartesian3,
} from "c137.js";

import "./style.css";

const Cesium = ({
  container,
  backgroundUrl,
  attribution,
  getExtent,
  accessToken,
  getTileset,
  getTerrainProvider,
  getStyle,
}) => {
  let viewer;

  Ion.defaultAccessToken = accessToken;

  if (getExtent) {
    Camera.DEFAULT_VIEW_RECTANGLE = getExtent(Rectangle);
    Camera.DEFAULT_VIEW_FACTOR = 0;
  }

  if (getTerrainProvider) {
    viewer = new Viewer(container, {
      imageryProvider: new WebMapTileServiceImageryProvider({
        url: backgroundUrl,
        layer: "Base",
        style: "default",
        tileMatrixSetID: "WebMercatorQuad",
      }),
      terrainProvider: getTerrainProvider(CesiumTerrainProvider, IonResource, Credit),
      animation: false,
      baseLayerPicker: false,
      homeButton: false,
      geocoder: false,
      timeline: false,
      scene3DOnly: true,
      fullscreenButton: false,
      navigationInstructionsInitiallyVisible: false,
    });
  } else {
    viewer = new Viewer(container, {
      imageryProvider: new WebMapTileServiceImageryProvider({
        url: backgroundUrl,
        layer: "Base",
        style: "default",
        tileMatrixSetID: "WebMercatorQuad",
      }),
      animation: false,
      baseLayerPicker: false,
      homeButton: true,
      geocoder: false,
      timeline: false,
      scene3DOnly: true,
      fullscreenButton: false,
      navigationInstructionsInitiallyVisible: false,
    });
  }

  if (attribution) {
    const credit = new Credit(attribution);
    viewer.scene.frameState.creditDisplay.addDefaultCredit(credit);
  }

  if (getTileset) {
    const tileset = getTileset(Cesium3DTileset, Matrix4, Cartesian3);
    if (getStyle) {
      tileset.style = getStyle(Cesium3DTileStyle);
    }
    console.log(tileset.modelMatrix);
    viewer.scene.primitives.add(tileset);
    viewer.flyTo(tileset);
  }
};

export default Cesium;
