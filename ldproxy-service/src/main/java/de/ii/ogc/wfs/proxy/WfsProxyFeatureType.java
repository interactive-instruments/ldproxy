package de.ii.ogc.wfs.proxy;

/**
 * @author zahnen
 */
public class WfsProxyFeatureType {
    private String name;
    private String namespace;
    private String displayName;
    private WfsProxyFeatureTypeMapping mappings;

    public WfsProxyFeatureType() {

    }
    public WfsProxyFeatureType(String name, String namespace, String displayName) {
        this.name = name;
        this.namespace = namespace;
        this.displayName = displayName;
        this.mappings = new WfsProxyFeatureTypeMapping();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public WfsProxyFeatureTypeMapping getMappings() {
        return mappings;
    }

    public void setMappings(WfsProxyFeatureTypeMapping mappings) {
        this.mappings = mappings;
    }
}
