/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_TYPE;


public class OgcApiFeaturesGenericMapping extends AbstractOgcApiFeaturesGenericMapping<OgcApiFeaturesGenericMapping.GENERIC_TYPE> {

    public enum GENERIC_TYPE {

        ID(GML_TYPE.ID),
        VALUE(GML_TYPE.STRING, GML_TYPE.URI, GML_TYPE.INT, GML_TYPE.INTEGER, GML_TYPE.LONG, GML_TYPE.SHORT, GML_TYPE.DECIMAL, GML_TYPE.DOUBLE, GML_TYPE.FLOAT, GML_TYPE.BOOLEAN),
        TEMPORAL(GML_TYPE.DATE, GML_TYPE.DATE_TIME),
        SPATIAL(),
        REFERENCE(),
        REFERENCE_EMBEDDED(),
        NONE(GML_TYPE.NONE);

        private GML_TYPE[] gmlTypes;

        GENERIC_TYPE(GML_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static GENERIC_TYPE forGmlType(GML_TYPE gmlType) {
            for (GENERIC_TYPE geoJsonType : GENERIC_TYPE.values()) {
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

    protected GENERIC_TYPE type;
    @Override
    public GENERIC_TYPE getType() {
        return type;
    }

    public void setType(GENERIC_TYPE type) {
        this.type = type;
    }

    @JsonIgnore
    public boolean isId() {
        return type == GENERIC_TYPE.ID;
    }

    @JsonIgnore
    public boolean isValue() {
        return type == GENERIC_TYPE.VALUE;
    }

    @Override
    public boolean isSpatial() {
        return type == GENERIC_TYPE.SPATIAL;
    }

    @JsonIgnore
    public boolean isTemporal() {
        return type == GENERIC_TYPE.TEMPORAL;
    }

    @JsonIgnore
    public boolean isReference() {
        return type == GENERIC_TYPE.REFERENCE;
    }

    @Override
    public boolean isReferenceEmbed() {
        return type == GENERIC_TYPE.REFERENCE_EMBEDDED;
    }
}
