/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class FeaturesFormatJsonFgCompatibility extends FeaturesFormatJsonFgBase {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.ogc.fg+json", ImmutableMap.of("compatibility", "geojson")))
            .label("JSON-FG (GeoJSON Compatibility Mode)")
            .parameter("jsonfgc")
            .fileExtension("fg.json")
            .build();

    public FeaturesFormatJsonFgCompatibility(@Requires SchemaGeneratorOpenApi schemaGeneratorFeature,
                                             @Requires SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
                                             @Requires GeoJsonWriterRegistry geoJsonWriterRegistry,
                                             @Requires CrsTransformerFactory crsTransformerFactory) {
        super(schemaGeneratorFeature, schemaGeneratorFeatureCollection, geoJsonWriterRegistry, crsTransformerFactory);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData)
            && apiData.getExtension(JsonFgConfiguration.class)
            .map(JsonFgConfiguration::getGeojsonCompatibility)
            .orElse(true);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return super.isEnabledForApi(apiData, collectionId)
            && apiData.getExtension(JsonFgConfiguration.class, collectionId)
            .map(JsonFgConfiguration::getGeojsonCompatibility)
            .orElse(true);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    protected boolean includePrimaryGeometry(FeatureTransformationContext transformationContext) {
        return true;
    }
}
