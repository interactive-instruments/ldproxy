/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiFeaturesOutputFormatGml implements ConformanceClass, OgcApiFeatureFormatExtension {

    private static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "gml+xml", ImmutableMap.of("version", "3.2", "profile", "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")))
            .label("GML")
            .parameter("xml")
            .build();
    public static final OgcApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    private final GmlConfig gmlConfig;
    private final OgcApiFeatureCoreProviders providers;

    @ServiceController(value = false)
    private boolean enable;

    public OgcApiFeaturesOutputFormatGml(@Requires GmlConfig gmlConfig,
                                         @Requires OgcApiFeatureCoreProviders providers) {
        this.gmlConfig = gmlConfig;
        this.providers = providers;
    }

    @Validate
    private void onStart() {
        this.enable = gmlConfig.isEnabled();
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaType getCollectionMediaType() {
        return COLLECTION_MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, GmlConfiguration.class);
    }

    @Override
    public boolean canPassThroughFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.of(new FeatureTransformerGmlUpgrade(ImmutableFeatureTransformationContextGml.builder()
                                                                                                    .from(transformationContext)
                                                                                                    .namespaces(((ConnectionInfoWfsHttp) providers.getFeatureProvider(transformationContext.getApiData())
                                                                                                                                                  .getData()
                                                                                                                                                              .getConnectionInfo())
                                                                                                            .getNamespaces())
                                                                                                    .build()));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.empty();
    }
}
