/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.flatgeobuf.domain;

import de.ii.ldproxy.ogcapi.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeatureCollectionOpenApi;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeatureOpenApi;
import de.ii.ldproxy.ogcapi.features.flatgeobuf.app.FeatureTransformerFlatgeobuf;
import de.ii.ldproxy.ogcapi.features.flatgeobuf.app.ImmutableFeatureTransformationContextFlatgeobuf;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.ws.rs.core.MediaType;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@Provides(specifications = {FeaturesFormatFlatgeobuf.class, FeatureFormatExtension.class, FormatExtension.class, ApiExtension.class})
@Instantiate
public class FeaturesFormatFlatgeobuf implements FeatureFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "flatgeobuf"))
            .label("Flatgeobuf")
            .parameter("fgb")
            .build();

    @Requires
    SchemaGeneratorFeatureSimpleFeature schemaGeneratorFeatureSimpleFeature;

    @Requires
    SchemaGeneratorFeatureOpenApi schemaGeneratorFeature;

    @Requires
    SchemaGeneratorFeatureCollectionOpenApi schemaGeneratorFeatureCollection;

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FlatgeobufConfiguration.class;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaType getCollectionMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        String schemaRef = "#/components/schemas/anyObject";
        Schema schema = new ObjectSchema();
        String collectionId = path.split("/", 4)[2];
        Optional<FlatgeobufConfiguration> configuration = apiData.getCollections()
                                                                 .get(collectionId)
                                                                 .getExtension(FlatgeobufConfiguration.class);
        Optional<Integer> maxMultiplicity = configuration.map(config -> config.getMaxMultiplicity());
        SchemaGeneratorFeature.SCHEMA_TYPE type = SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT;
        if (path.matches("/collections/[^//]+/items/?")) {
            schemaRef = schemaGeneratorFeatureCollection.getSchemaReferenceOpenApi(collectionId, type);
            schema = schemaGeneratorFeatureCollection.getSchemaOpenApi(apiData, collectionId, type);
        } else if (path.matches("/collections/[^//]+/items/[^//]+/?")) {
            schemaRef = schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId, type);
            schema = schemaGeneratorFeature.getSchemaOpenApi(apiData, collectionId, type);
        }
        // TODO example
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext,
                                                               Optional<Locale> language) {

        OgcApiDataV2 apiData = transformationContext.getApiData();
        String collectionId = transformationContext.getCollectionId();
        int maxMultiplicity = apiData.getCollections().get(collectionId).getExtension(FlatgeobufConfiguration.class).get().getMaxMultiplicity();
        CoordinateReferenceSystem crs = null;
        try {
            if (transformationContext.getCrsTransformer().isPresent())
                crs = CRS.decode("EPSG:"+transformationContext.getCrsTransformer().get().getTargetCrs().getCode());
        } catch (Exception e) {
            // use the null value
        }
        Map<String,Class> properties = schemaGeneratorFeatureSimpleFeature.getSchemaSimpleFeature(apiData, collectionId, maxMultiplicity, crs,
                                                                                              transformationContext.getFields(),
                                                                                              SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT);

        return Optional.of(new FeatureTransformerFlatgeobuf(ImmutableFeatureTransformationContextFlatgeobuf.builder()
                                                                                                           .from(transformationContext)
                                                                                                           .simpleFeatureType(properties)
                                                                                                           .build()));
    }

}
