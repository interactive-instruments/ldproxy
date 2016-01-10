package de.ii.ldproxy.output.jsonld;

import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public class JsonLdMapping implements TargetMapping {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
