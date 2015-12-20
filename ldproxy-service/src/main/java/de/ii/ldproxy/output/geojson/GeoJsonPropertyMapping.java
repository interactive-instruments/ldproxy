package de.ii.ldproxy.output.geojson;

import static de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE;

/**
 * @author zahnen
 */
public class GeoJsonPropertyMapping implements GeoJsonMapping {

    public enum GEO_JSON_TYPE {

        ID(GML_TYPE.ID),
        STRING(GML_TYPE.STRING, GML_TYPE.DATE, GML_TYPE.DATE_TIME),
        NUMBER(GML_TYPE.INT, GML_TYPE.INTEGER, GML_TYPE.DECIMAL, GML_TYPE.DOUBLE),
        GEOMETRY(),
        NONE(GML_TYPE.NONE);

        private GML_TYPE[] gmlTypes;

        GEO_JSON_TYPE(GML_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static GEO_JSON_TYPE forGmlType(GML_TYPE gmlType) {
            for (GEO_JSON_TYPE geoJsonType : GEO_JSON_TYPE.values()) {
                for (GML_TYPE v2: geoJsonType.gmlTypes) {
                    if (v2 == gmlType) {
                        return geoJsonType;
                    }
                }
            }

            return NONE;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    private boolean enabled;
    private String name;
    private GEO_JSON_TYPE type;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public GEO_JSON_TYPE getType() {
        return type;
    }

    public void setType(GEO_JSON_TYPE type) {
        this.type = type;
    }
}
