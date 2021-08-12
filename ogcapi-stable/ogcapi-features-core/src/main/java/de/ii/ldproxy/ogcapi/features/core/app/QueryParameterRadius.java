/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterRadius extends ApiExtensionCache implements OgcApiQueryParameter {

    // TODO this is a temporary extension for Testbed-17, which should not be merged;
    //      if we add support for a center point / radius filter, it should be implemented
    //      using ST_BUFFER() and not be based on nautical miles

    private final Schema baseSchema;

    public QueryParameterRadius() {
        baseSchema = new NumberSchema().format("double")
                                       .minimum(new BigDecimal(0));
        baseSchema.setDefault(new BigDecimal(1));
    }

    @Override
    public String getName() {
        return "radius";
    }

    @Override
    public String getDescription() {
        return "Features that have a geometry that intersects a circle are selected. " +
                "The radius is provided in nautical miles with a default of one nautical mile.\n" +
                "The center of the circle is provided in the parameter 'center'.\n" +
                "If a feature has multiple spatial geometry properties, it is the decision of the server " +
                "whether only a single spatial geometry property is used to determine the extent or all relevant geometries.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/collections/{collectionId}/items"));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
