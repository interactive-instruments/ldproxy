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
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterBbox extends ApiExtensionCache implements OgcApiQueryParameter {

    private final Schema<?> baseSchema;
    private final SchemaValidator schemaValidator;

    @Inject
    public QueryParameterBbox(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
        // TODO support 6 coordinates (note: maxItems was originally set to 4 for now, but the CITE tests require maxItems=6)
        this.baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(6);
    }

    @Override
    public String getName() {
        return "bbox";
    }

    @Override
    public String getDescription() {
        return "Only features that have a geometry that intersects the bounding box are selected. " +
                "The bounding box is provided as four numbers:\n\n" +
                "* Lower left corner, coordinate axis 1 \n" +
                "* Lower left corner, coordinate axis 2 \n" +
                "* Upper right corner, coordinate axis 1 \n" +
                "* Upper right corner, coordinate axis 2 \n" +
                "The coordinate reference system of the values is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) " +
                "unless a different coordinate reference system is specified in the parameter `bbox-crs`. " +
                "For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, " +
                "minimum latitude, maximum longitude and maximum latitude. " +
                "However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger " +
                "than the third value (east-most box edge).";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/collections/{collectionId}/items"));
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
