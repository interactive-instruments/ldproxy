/* eslint-disable no-undef, no-underscore-dangle */
import Cesium from "../../components/Cesium";

if (globalThis._map && globalThis._cesium && globalThis._map.container) {
  globalThis.CESIUM_BASE_URL = globalThis._cesium.assetsPrefix + process.env.CESIUM_PATH;

  Cesium({ ...globalThis._map, ...globalThis._cesium });
}
