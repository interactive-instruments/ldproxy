/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.base.domain.AppContext;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn Debug option in development environments: Log debug information for the GeoJSON output.
 * @langDe Todo
 * @name debug
 * @endpoints Features
 */
@Singleton
@AutoBind
public class QueryParameterDebugFeaturesGeoJson extends ApiExtensionCache implements
    OgcApiQueryParameter {

    private final Schema<?> schema = new BooleanSchema()._default(false);
    private final boolean allowDebug;
    private final SchemaValidator schemaValidator;

    @Inject
    public QueryParameterDebugFeaturesGeoJson(AppContext appContext, SchemaValidator schemaValidator) {
        this.allowDebug = appContext.isDevEnv();
        this.schemaValidator = schemaValidator;
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "Debug option in development environments: Log debug information for the GeoJSON output.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return OgcApiQueryParameter.super.isEnabledForApi(apiData) && allowDebug;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return OgcApiQueryParameter.super.isEnabledForApi(apiData, collectionId) && allowDebug;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonConfiguration.class;
    }
}
