/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.OgcApiCollectionExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtent;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.xtraplatform.crs.api.BoundingBox;
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

    public OgcApiFeaturesCollectionExtension(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
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

        // TODO add support for schemas
        Optional<String> describeFeatureTypeUrl = Optional.empty();
        if (describeFeatureTypeUrl.isPresent()) {
            collection.addLinks(new ImmutableOgcApiLink.Builder()
                    .href(describeFeatureTypeUrl.get())
                    .rel("describedBy")
                    .type("application/xml")
                    .title(i18n.get("describedByXsdLink",language))
                    .build());
        }

        // only add extents for cases where we can filter using spatial / temporal predicates
        Optional<OgcApiFeaturesCollectionQueryables> queryables = getExtensionConfiguration(featureType, OgcApiFeaturesCoreConfiguration.class).flatMap(OgcApiFeaturesCoreConfiguration::getQueryables);
        boolean hasSpatialQueryable = queryables.map(OgcApiFeaturesCollectionQueryables::getSpatial)
                                                .filter(spatial -> !spatial.isEmpty())
                                                .isPresent();
        boolean hasTemporalQueryable = queryables.map(OgcApiFeaturesCollectionQueryables::getTemporal)
                                                .filter(temporal -> !temporal.isEmpty())
                                                .isPresent();
        if (hasSpatialQueryable && hasTemporalQueryable) {
            BoundingBox spatial = featureType
                    .getExtent()
                    .getSpatial();
            FeatureTypeConfigurationOgcApi.TemporalExtent temporal = featureType
                    .getExtent()
                    .getTemporal();
            collection.extent(new OgcApiExtent(
                    temporal.getStart(),
                    temporal.getEnd(),
                    spatial.getXmin(),
                    spatial.getYmin(),
                    spatial.getXmax(),
                    spatial.getYmax()));
        } else if (hasSpatialQueryable) {
            BoundingBox spatial = featureType.getExtent()
                    .getSpatial();
            collection.extent(new OgcApiExtent(
                    spatial.getXmin(),
                    spatial.getYmin(),
                    spatial.getXmax(),
                    spatial.getYmax()));
        } else if (hasTemporalQueryable) {
            FeatureTypeConfigurationOgcApi.TemporalExtent temporal = featureType.getExtent()
                    .getTemporal();
            collection.extent(new OgcApiExtent(
                    temporal.getStart(),
                    temporal.getEnd()));
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
