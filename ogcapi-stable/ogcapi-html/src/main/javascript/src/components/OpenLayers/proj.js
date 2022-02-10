import proj4 from "proj4";
import { get } from "ol/proj";
import { register } from "ol/proj/proj4";

export const setupProjections = () => {
  proj4.defs(
    "EPSG:25832",
    "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"
  );
  proj4.defs(
    "EPSG:3395",
    "+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs"
  );

  register(proj4);

  get("EPSG:25832").setExtent([
    -46133.17, 5048875.268576, 1206211.101424, 6301219.54,
  ]);
  get("EPSG:3395").setExtent([
    -20037508.342789244, -20037508.342789244, 20037508.342789244,
    20037508.342789244,
  ]);
};
