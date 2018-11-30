package de.ii.ldproxy.wfs3.api;

import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;

import java.util.Map;

/**
 * @author zahnen
 */
public interface Wfs3ParameterExtension extends Wfs3Extension {

    Map<String, String> getParameters();

    Map<String, String> transformParameters(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, Map<String, String> parameters);

    void transformQuery(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, ImmutableFeatureQuery.Builder queryBuilder);
}
