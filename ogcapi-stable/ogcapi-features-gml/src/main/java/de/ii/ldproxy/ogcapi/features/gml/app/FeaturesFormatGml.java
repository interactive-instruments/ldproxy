/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.gml.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class FeaturesFormatGml implements ConformanceClass, FeatureFormatExtension {

    private static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "gml+xml", ImmutableMap.of("version", "3.2", "profile", "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")))
            .label("GML")
            .parameter("xml")
            .build();
    public static final ApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    private final FeaturesCoreProviders providers;

    public FeaturesFormatGml(@Requires FeaturesCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2");
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getCollectionMediaType() {
        return COLLECTION_MEDIA_TYPE;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GmlConfiguration.class;
    }

    @Override
    public boolean canPassThroughFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.of(new FeatureTransformerGmlUpgrade(ImmutableFeatureTransformationContextGml.builder()
                                                                                                    .from(transformationContext)
                                                                                                    .namespaces(((ConnectionInfoWfsHttp) ((WithConnectionInfo<?>)providers.getFeatureProviderOrThrow(transformationContext.getApiData())
                                                                                                                                                                          .getData()).getConnectionInfo()).getNamespaces())
                                                                                                    .build()));
    }

}
