/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.target.geojson.SchemaGeneratorFeatureCollection;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.api.FeatureTransformerObservationProcessing;
import de.ii.ldproxy.ogcapi.observation_processing.api.ImmutableFeatureTransformationContextObservationProcessing;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormat;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.akka.http.Http;
import org.apache.felix.ipojo.annotations.*;

import javax.ws.rs.core.MediaType;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@Provides(specifications = {OutputFormatGeoJson.class, ObservationProcessingOutputFormat.class, FormatExtension.class, OgcApiExtension.class})
@Instantiate
public class OutputFormatGeoJson implements ObservationProcessingOutputFormat {

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "geo+json"))
            .label("GeoJSON")
            .parameter("json")
            .build();

    @Requires
    private Dropwizard dropwizard;

    @Requires
    private I18n i18n;

    @Requires
    private CodelistRegistry codelistRegistry;

    @Requires
    private OgcApiFeatureCoreProviders providers;

    @Requires
    private Http http;

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        OgcApiApiDataV2 serviceData = transformationContext.getApiData();
        String collectionName = transformationContext.getCollectionId();
        String staticUrlPrefix = transformationContext.getOgcApiRequest()
                                                      .getStaticUrlPrefix();
        URICustomizer uriCustomizer = transformationContext.getOgcApiRequest()
                                                           .getUriCustomizer();

        if (transformationContext.isFeatureCollection()) {
            FeatureTypeConfigurationOgcApi collectionData = serviceData.getCollections()
                                                                       .get(collectionName);
            Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(OgcApiFeaturesCoreConfiguration.class);
            Optional<ObservationProcessingConfiguration> obsProcConfiguration = collectionData.getExtension(ObservationProcessingConfiguration.class);
            FeatureProviderDataV1 providerData = providers.getFeatureProvider(serviceData, collectionData)
                                                          .getData();

            Map<String, String> filterableFields = featuresCoreConfiguration
                                                                 .map(OgcApiFeaturesCoreConfiguration::getOtherFilterParameters)
                                                                 .orElse(ImmutableMap.of());

        } else {
            // TODO throw error
        }

        ImmutableFeatureTransformationContextObservationProcessing transformationContextObsProc = new ImmutableFeatureTransformationContextObservationProcessing.Builder()
                .from(transformationContext)
                .codelists(codelistRegistry.getCodelists())
                // TODO .mustacheRenderer(dropwizard.getMustacheRenderer())
                .i18n(i18n)
                .language(language)
                .build();

        FeatureTransformer2 transformer = new FeatureTransformerObservationProcessing(transformationContextObsProc, http.getDefaultClient());

        return Optional.of(transformer);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        // TODO specific schemas and example
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(SchemaGeneratorFeatureCollection.getGeneric())
                .schemaRef(SchemaGeneratorFeatureCollection.referenceGeneric())
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return true;
    }

    @Override
    public boolean contentPerResource() {
        return true;
    }
}
