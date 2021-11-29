/* eslint-disable no-undef, no-underscore-dangle */
import Cesium from "../../components/Cesium";

if (globalThis._map && globalThis._cesium && globalThis._map.container) {
  Cesium({ ...globalThis._map, ...globalThis._cesium });
}
