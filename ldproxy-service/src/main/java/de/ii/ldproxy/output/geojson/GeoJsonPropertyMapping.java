package de.ii.ldproxy.output.geojson;

/**
 * @author zahnen
 */
public class GeoJsonPropertyMapping implements GeoJsonMapping {

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
