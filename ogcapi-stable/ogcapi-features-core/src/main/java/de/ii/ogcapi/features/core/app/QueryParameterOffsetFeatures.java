/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.math.BigDecimal;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterOffsetFeatures extends ApiExtensionCache implements OgcApiQueryParameter {

    @Inject
    QueryParameterOffsetFeatures() {
    }

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
