/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.geometry_simplification;

import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterClipbox extends ApiExtensionCache implements OgcApiQueryParameter {

    private final FeaturesCoreProviders providers;
    private final Schema baseSchema;

    public QueryParameterClipbox(@Requires FeaturesCoreProviders providers) {
        this.providers = providers;
        baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(4);
    }

    @Override
    public String getName() {
        return "clip-box";
    }

    @Override
    public String getDescription() {
        return "Clip feature geometries to the part of their geometry that intersects the specified bounding box. " +
                "The bounding box is provided as four numbers:\n\n" +
                "* Lower left corner, coordinate axis 1 \n" +
                "* Lower left corner, coordinate axis 2 \n" +
                "* Upper right corner, coordinate axis 1 \n" +
                "* Upper right corner, coordinate axis 2 \n\n" +
                "The coordinate reference system of the values is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) " +
                "unless a different coordinate reference system is specified in the parameter `bbox-crs`.";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) &&
            providers.getFeatureProvider(apiData)
                .map(FeatureProvider2::supportsClipping)
                .orElse(false);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return super.isEnabledForApi(apiData, collectionId) &&
            providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId))
                .map(FeatureProvider2::supportsClipping)
                .orElse(false);
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
