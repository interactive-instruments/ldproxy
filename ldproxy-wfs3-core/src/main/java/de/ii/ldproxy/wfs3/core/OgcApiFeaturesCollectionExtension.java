/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.api.OgcApiFeatureFormatExtension;
import de.ii.xtraplatform.crs.api.BoundingBox;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiFeaturesCollectionExtension implements OgcApiCollectionExtension {

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiFeaturesCollectionExtension(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection, FeatureTypeConfigurationOgcApi featureType, OgcApiDatasetData apiData, URICustomizer uriCustomizer, boolean isNested, OgcApiMediaType mediaType, List<OgcApiMediaType> alternateMediaTypes) {

        List<OgcApiMediaType> featureMediaTypes = extensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                .stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(outputFormatExtension -> outputFormatExtension.getMediaType())
                .collect(Collectors.toList());

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
                            .removeParameters("f")
                            .toString())
                    .rel("self")
                    .description("The collection '" + featureType.getLabel() +"'")
                    .build());
        }

        collection.addLinks(new ImmutableOgcApiLink.Builder()
                .href(uriBuilder.ensureLastPathSegment("items")
                        .toString())
                .rel("items")
                .description("Features in the collection '" + featureType.getLabel() +"'")
                .build());

        // TODO add support for schemas
        Optional<String> describeFeatureTypeUrl = Optional.empty();
        if (describeFeatureTypeUrl.isPresent()) {
            collection.addLinks(new ImmutableOgcApiLink.Builder()
                    .href(describeFeatureTypeUrl.get())
                    .rel("describedBy")
                    .type("application/xml")
                    .description("XML schema for the feature collection")
                    .build());
        }

        // only add extents for cases where we can filter using spatial / temporal predicates
        Map<String, String> filters = apiData.getFilterableFieldsForFeatureType(featureType.getId());
        if (filters.containsKey("bbox") && filters.containsKey("datetime")) {
            BoundingBox spatial = featureType
                    .getExtent()
                    .getSpatial();
            FeatureTypeConfigurationOgcApi.TemporalExtent temporal = featureType
                    .getExtent()
                    .getTemporal();
            collection.extent(new OgcApiExtent(
                    temporal.getStart(),
                    temporal.getEnd() == 0 ? temporal.getComputedEnd() : temporal.getEnd(),
                    spatial.getXmin(),
                    spatial.getYmin(),
                    spatial.getXmax(),
                    spatial.getYmax()));
        } else if (filters.containsKey("bbox")) {
            BoundingBox spatial = featureType.getExtent()
                    .getSpatial();
            collection.extent(new OgcApiExtent(
                    spatial.getXmin(),
                    spatial.getYmin(),
                    spatial.getXmax(),
                    spatial.getYmax()));
        } else if (filters.containsKey("datetime")) {
            FeatureTypeConfigurationOgcApi.TemporalExtent temporal = featureType.getExtent()
                    .getTemporal();
            collection.extent(new OgcApiExtent(
                    temporal.getStart(),
                    temporal.getEnd() == 0 ? temporal.getComputedEnd() : temporal.getEnd()));
        }

        return collection;
    }

    public static OgcApiCollection createNestedCollection(FeatureTypeConfigurationOgcApi featureType, OgcApiDatasetData apiData,
                                                   OgcApiMediaType mediaType, List<OgcApiMediaType> alternateMediaTypes,
                                                   URICustomizer uriCustomizer, List<OgcApiCollectionExtension> collectionExtenders) {
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
                    alternateMediaTypes);
        }

        return ogcApiCollection.build();
    }

}
