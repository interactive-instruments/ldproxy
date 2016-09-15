/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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