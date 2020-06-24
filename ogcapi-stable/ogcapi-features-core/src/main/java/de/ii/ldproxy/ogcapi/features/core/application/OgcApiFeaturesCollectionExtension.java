/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiFeaturesCollectionExtension implements OgcApiCollectionExtension {

    @Requires
    I18n i18n;

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiFeaturesCollectionExtension(@Requires OgcApiExtensionRegistry extensionRegistry,
                                             @Requires OgcApiFeatureCoreProviders providers) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureType,
                                                     OgcApiApiDataV2 apiData, URICustomizer uriCustomizer,
                                                     boolean isNested, OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {

        collection.title(featureType.getLabel())
                  .description(featureType.getDescription());

        URICustomizer uriBuilder = uriCustomizer
                .copy()
                .ensureNoTrailingSlash()
                .clearParameters();

        if (isNested) {
            // also add an untyped a self link in the Collections resource, otherwise the standard links are already there
            collection.addLinks(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder
                            .copy()
                            .ensureLastPathSegments("collections", featureType.getId())
                            .removeParameters("f")
                            .toString())
                    .rel("self")
                    .title(i18n.get("selfLinkCollection", language)
                               .replace("{{collection}}", featureType.getLabel()))
                    .build());
        }

        List<OgcApiMediaType> featureMediaTypes = extensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                                                   .stream()
                                                                   .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                                                                   .map(outputFormatExtension -> outputFormatExtension.getMediaType())
                                                                   .collect(Collectors.toList());

        featureMediaTypes
                .stream()
                .forEach(mtype -> collection.addLinks(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.ensureLastPathSegments("collections", featureType.getId(), "items")
                                        .setParameter("f", mtype.parameter())
                                        .toString())
                        .rel("items")
                        .type(mtype.type()
                                   .toString())
                        .title(i18n.get("itemsLink", language)
                                   .replace("{{collection}}", featureType.getLabel()))
                        .build()));

        Optional<String> describeFeatureTypeUrl = Optional.empty();
        if (describeFeatureTypeUrl.isPresent()) {
            collection.addLinks(new ImmutableOgcApiLink.Builder()
                    .href(describeFeatureTypeUrl.get())
                    .rel("describedby")
                    .type("application/xml")
                    .title(i18n.get("describedByXsdLink", language))
                    .build());
        }

        Optional<OgcApiFeaturesCoreConfiguration> config = getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class);
        if(config.isPresent() && config.get().getAdditionalLinks().containsKey("/collections/"+featureType.getId())) {
            List<OgcApiLink> additionalLinks = config.get().getAdditionalLinks().get("/collections/"+featureType.getId());
            additionalLinks.stream().forEach(link -> collection.addLinks(link));
        }

        // only add extents for cases where we can filter using spatial / temporal predicates
        Optional<OgcApiFeaturesCollectionQueryables> queryables = getExtensionConfiguration(featureType, OgcApiFeaturesCoreConfiguration.class).flatMap(OgcApiFeaturesCoreConfiguration::getQueryables);
        boolean hasSpatialQueryable = queryables.map(OgcApiFeaturesCollectionQueryables::getSpatial)
                                                .filter(spatial -> !spatial.isEmpty())
                                                .isPresent();
        boolean hasTemporalQueryable = queryables.map(OgcApiFeaturesCollectionQueryables::getTemporal)
                                                 .filter(temporal -> !temporal.isEmpty())
                                                 .isPresent();
        Optional<BoundingBox> spatial = featureType.getExtent().isPresent() ?
                                            featureType.getExtent()
                                                       .get()
                                                       .getSpatial() :
                                            Optional.empty();
        Optional<FeatureTypeConfigurationOgcApi.TemporalExtent> temporal = featureType.getExtent().isPresent() ?
                                            featureType.getExtent()
                                                       .get()
                                                       .getTemporal() :
                                            Optional.empty();
        if (hasSpatialQueryable && hasTemporalQueryable && spatial.isPresent() && temporal.isPresent()) {
            collection.extent(new OgcApiExtent(
                    temporal.get()
                            .getStart(),
                    temporal.get()
                            .getEnd(),
                    spatial.get()
                           .getXmin(),
                    spatial.get()
                           .getYmin(),
                    spatial.get()
                           .getXmax(),
                    spatial.get()
                           .getYmax()));
        } else if (hasSpatialQueryable && spatial.isPresent()) {
            collection.extent(new OgcApiExtent(
                    spatial.get()
                           .getXmin(),
                    spatial.get()
                           .getYmin(),
                    spatial.get()
                           .getXmax(),
                    spatial.get()
                           .getYmax()));
        } else if (hasTemporalQueryable && temporal.isPresent()) {
            collection.extent(new OgcApiExtent(
                    temporal.get()
                            .getStart(),
                    temporal.get()
                            .getEnd()));
        } else {
            collection.extent(new OgcApiExtent());
        }

        return collection;
    }

    public static OgcApiCollection createNestedCollection(FeatureTypeConfigurationOgcApi featureType,
                                                          OgcApiApiDataV2 apiData,
                                                          OgcApiMediaType mediaType,
                                                          List<OgcApiMediaType> alternateMediaTypes,
                                                          Optional<Locale> language,
                                                          URICustomizer uriCustomizer,
                                                          List<OgcApiCollectionExtension> collectionExtenders) {
        ImmutableOgcApiCollection.Builder ogcApiCollection = ImmutableOgcApiCollection.builder()
                                                                                      .id(featureType.getId());

        for (OgcApiCollectionExtension ogcApiCollectionExtension : collectionExtenders) {
            ogcApiCollection = ogcApiCollectionExtension.process(
                    ogcApiCollection,
                    featureType,
                    apiData,
                    uriCustomizer.copy(),
                    true,
                    mediaType,
                    alternateMediaTypes,
                    language);
        }

        ImmutableOgcApiCollection result = null;
        try {
            result = ogcApiCollection.build();
        } catch (Throwable e) {
            result = null;
        }
        return result;
    }

}
