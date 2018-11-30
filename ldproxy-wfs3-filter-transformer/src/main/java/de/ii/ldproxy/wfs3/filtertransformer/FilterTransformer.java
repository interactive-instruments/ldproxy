package de.ii.ldproxy.wfs3.filtertransformer;

import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;

import java.util.Map;

/**
 * @author zahnen
 */
public interface FilterTransformer {

    Map<String, String> resolveParameters(Map<String, String> parameters);
}
