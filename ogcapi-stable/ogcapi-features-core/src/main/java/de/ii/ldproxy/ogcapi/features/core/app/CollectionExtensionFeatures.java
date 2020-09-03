/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiExtent;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
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
public class CollectionExtensionFeatures implements CollectionExtension {

    @Requires
    I18n i18n;

    private final ExtensionRegistry extensionRegistry;

    public CollectionExtensionFeatures(@Requires ExtensionRegistry extensionRegistry,
                                       @Requires FeaturesCoreProviders providers) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureType,
                                                     OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                                     boolean isNested, ApiMediaType mediaType,
                                                     List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {

        collection.title(featureType.getLabel())
                  .description(featureType.getDescription());

        URICustomizer uriBuilder = uriCustomizer
                .copy()
                .ensureNoTrailingSlash()
                .clearParameters();

        if (isNested) {
            // also add an untyped a self link in the Collections resource, otherwise the standard links are already there
            collection.addLinks(new ImmutableLink.Builder()
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

        List<ApiMediaType> featureMediaTypes = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class)
                                                                .stream()
                                                                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                                                                .map(outputFormatExtension -> outputFormatExtension.getMediaType())
                                                                .collect(Collectors.toList());

        featureMediaTypes
                .stream()
                .forEach(mtype -> collection.addLinks(new ImmutableLink.Builder()
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
            collection.addLinks(new ImmutableLink.Builder()
                    .href(describeFeatureTypeUrl.get())
                    .rel("describedby")
                    .type("application/xml")
                    .title(i18n.get("describedByXsdLink", language))
                    .build());
        }

        // only add extents for cases where we can filter using spatial / temporal predicates
        Optional<FeaturesCollectionQueryables> queryables = featureType.getExtension(FeaturesCoreConfiguration.class).flatMap(FeaturesCoreConfiguration::getQueryables);
        boolean hasSpatialQueryable = queryables.map(FeaturesCollectionQueryables::getSpatial)
                                                .filter(spatial -> !spatial.isEmpty())
                                                .isPresent();
        boolean hasTemporalQueryable = queryables.map(FeaturesCollectionQueryables::getTemporal)
                                                 .filter(temporal -> !temporal.isEmpty())
                                                 .isPresent();
        Optional<BoundingBox> spatial = apiData.getSpatialExtent(featureType.getId());
        Optional<TemporalExtent> temporal = apiData.getTemporalExtent(featureType.getId());
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
                                                          OgcApiDataV2 apiData,
                                                          ApiMediaType mediaType,
                                                          List<ApiMediaType> alternateMediaTypes,
                                                          Optional<Locale> language,
                                                          URICustomizer uriCustomizer,
                                                          List<CollectionExtension> collectionExtenders) {
        ImmutableOgcApiCollection.Builder ogcApiCollection = ImmutableOgcApiCollection.builder()
                                                                                      .id(featureType.getId());

        for (CollectionExtension ogcApiCollectionExtension : collectionExtenders) {
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
