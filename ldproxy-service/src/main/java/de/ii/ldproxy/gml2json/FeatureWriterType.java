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
                return new EsriJsonIdArrayWriter(layer, json);
            } else {
                return new EsriJsonFeatureWriter(layer, json, queryParams, singleFeature);
            }
        }
    },
    GEO_JSON("geojson") {
        @Override
        protected GMLAnalyzer create() {
            return new GeoJsonFeatureWriter(layer, json, queryParams, singleFeature);
        }
    };
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FeatureWriterType.class);
    protected String stringRepresentation;
    protected WFS2GSFSLayer layer;
    protected JsonGenerator json;
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
        this.json = null;
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

    public FeatureWriterType json(JsonGenerator json) {
        this.json = json;
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