package de.ii.ogc.wfs.proxy;

/**
 * @author zahnen
 */
public class WFSProxyServiceProperties {
    private int maxFeatures;

    public WFSProxyServiceProperties(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public int getMaxFeatures() {
        return maxFeatures;
    }

    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }
}
