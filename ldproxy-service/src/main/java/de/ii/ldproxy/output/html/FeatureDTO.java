package de.ii.ldproxy.output.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class FeatureDTO {
    public String displayName;
    public List<Map<String,String>> fields;
    public boolean geo;
    public String lat;
    public String lon;
    public boolean details;

    public FeatureDTO() {
        this.fields = new ArrayList<>();
    }
}
