/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

/**
 *
 * @author zahnen
 */
public enum FeatureWriterType {

    /*ESRI_JSON("esrijson") {
        @Override
        protected GMLAnalyzer create() {
            if (idsOnly) {
                return new EsriJsonIdArrayWriter(layer, jsonOut);
            } else {
                return new EsriJsonFeatureWriter(layer, jsonOut, queryParams, singleFeature);
            }
        }
    },
    GEO_JSON("geojson") {
        @Override
        protected GMLAnalyzer create() {
            return new GeoJsonFeatureWriter(layer, jsonOut, queryParams, singleFeature);
        }
    };
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FeatureWriterType.class);
    protected String stringRepresentation;
    protected WFS2GSFSLayer layer;
    protected JsonGenerator jsonOut;
    protected LayerQueryParameters queryParams;
    protected boolean singleFeature;
    protected boolean idsOnly;

    private FeatureWriterType(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
        this.singleFeature = false;
        this.idsOnly = false;
    }

    protected abstract GMLAnalyzer create();

    public GMLAnalyzer build() {
        if (queryParams == null) {
            queryParams = new LayerQueryParameters();
        }
        return create();
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }
    
    private FeatureWriterType clean() {
        this.singleFeature = false;
        this.idsOnly = false;
        this.jsonOut = null;
        this.layer = null;
        this.queryParams = null;
        return this;
    }

    public static FeatureWriterType fromString(String type) {
        if (type != null) {
            for (FeatureWriterType t : FeatureWriterType.values()) {
                if (t.toString().equals(type.toLowerCase())) {
                    return t.clean();
                }
            }
        }
        return ESRI_JSON.clean();
    }

    public FeatureWriterType layer(WFS2GSFSLayer layer) {
        this.layer = layer;
        return this;
    }

    public FeatureWriterType jsonOut(JsonGenerator jsonOut) {
        this.jsonOut = jsonOut;
        return this;
    }

    public FeatureWriterType queryParams(LayerQueryParameters queryParams) {
        this.queryParams = queryParams;
        if (queryParams.getReturnIdsOnly()) {
            idsOnly();
        }
        return this;
    }

    public FeatureWriterType singleFeature() {
        this.singleFeature = true;
        return this;
    }

    public FeatureWriterType idsOnly() {
        this.idsOnly = true;
        return this;
    }*/
}