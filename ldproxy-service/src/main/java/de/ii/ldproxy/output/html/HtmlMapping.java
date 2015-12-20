package de.ii.ldproxy.output.html;

import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public class HtmlMapping implements TargetMapping {

    private boolean enabled;
    private String name;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
