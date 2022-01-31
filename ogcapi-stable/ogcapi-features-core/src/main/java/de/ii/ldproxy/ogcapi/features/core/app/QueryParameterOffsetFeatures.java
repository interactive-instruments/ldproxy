/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;

@Component
@Provides
@Instantiate
public class QueryParameterOffsetFeatures extends ApiExtensionCache implements OgcApiQueryParameter {

    @Override
    public String getId() {
        return "offsetFeatures";
    }

    @Override
    public String getName() {
        return "offset";
    }

    @Override
    public String getDescription() {
        return "The optional offset parameter identifies the index of the first feature in the response in the overall " +
                "result set.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items"));
    }

    private Schema schema = null;

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        if (schema==null) {
            schema = new IntegerSchema()._default(0).minimum(BigDecimal.ZERO);
        }
        return schema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
