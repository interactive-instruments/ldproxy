package de.ii.ldproxy.service;

import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public class IndexMapping implements TargetMapping {
    public static final String MIME_TYPE = "index";


    private boolean enabled;
    private String name;

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
    public boolean isGeometry() {
        return false;
    }
}
