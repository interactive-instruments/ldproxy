package de.ii.ldproxy.output.html;

import de.ii.ogc.wfs.proxy.TargetMapping;

import static de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE;

/**
 * @author zahnen
 */
public interface MicrodataMapping extends TargetMapping {
    MICRODATA_TYPE getType();
    boolean isShowInCollection();
    String getItemType();
    String getItemProp();

    enum MICRODATA_TYPE {

        ID(GML_TYPE.ID),
        STRING(GML_TYPE.STRING, GML_TYPE.DATE, GML_TYPE.DATE_TIME),
        NUMBER(GML_TYPE.INT, GML_TYPE.INTEGER, GML_TYPE.DECIMAL, GML_TYPE.DOUBLE),
        GEOMETRY(),
        NONE(GML_TYPE.NONE);

        private GML_TYPE[] gmlTypes;

        MICRODATA_TYPE(GML_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static MICRODATA_TYPE forGmlType(GML_TYPE gmlType) {
            for (MICRODATA_TYPE geoJsonType : MICRODATA_TYPE.values()) {
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
}
