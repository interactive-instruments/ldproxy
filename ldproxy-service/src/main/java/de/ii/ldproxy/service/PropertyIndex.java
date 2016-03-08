package de.ii.ldproxy.service;

import de.ii.xsf.core.api.Resource;

import java.util.List;

/**
 * @author zahnen
 */
public class PropertyIndex implements Resource {

    private String propertyName;
    private List<String> values;

    @Override
    public String getResourceId() {
        return propertyName;
    }

    @Override
    public void setResourceId(String resourceId) {
        this.propertyName = resourceId;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
