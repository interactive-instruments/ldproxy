/* eslint-disable no-undef, no-underscore-dangle */
import Cesium from "../../components/Cesium";

if (globalThis._map && globalThis._cesium && globalThis._map.container) {
  globalThis.CESIUM_BASE_URL = `${window.location.protocol}//${window.location.host}`;
  Cesium({ ...globalThis._map, ...globalThis._cesium });
}
