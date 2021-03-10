/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class CollectionsOnLandingPage implements LandingPageExtension {

    @Requires
    I18n i18n;

    private final ExtensionRegistry extensionRegistry;

    public CollectionsOnLandingPage(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CollectionsConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        List<String> collectionNames = apiData.getCollections()
                .values()
                .stream()
                .filter(featureType -> featureType.getEnabled())
                .map(featureType -> featureType.getLabel())
                .collect(Collectors.toList());
        String suffix = (collectionNames.size()>0 && collectionNames.size()<=4) ? " ("+String.join(", ", collectionNames)+")" : "";

        landingPageBuilder.addLinks(new ImmutableLink.Builder()
                        .href(uriCustomizer.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment("collections")
                                .removeParameters("f")
                                .toString())
                        .rel("data")
                        .title(i18n.get("dataLink",language) + suffix)
                        .build());

        ImmutableList.Builder<Link> distributionLinks = new ImmutableList.Builder<Link>()
                .addAll(apiData.getExtension(CollectionsConfiguration.class)
                               .map(CollectionsConfiguration::getAdditionalLinks)
                               .orElse(ImmutableList.<Link>of())
                               .stream()
                               .filter(link -> Objects.equals(link.getRel(), "enclosure"))
                               .collect(Collectors.toUnmodifiableList()));

        // for cases with a single collection, that collection is not reported as a sub-dataset and we need to
        // determine the distribution links (enclosure links provided in additonalLinks and the regular items
        // links to the features in the API)
        if (apiData.getCollections().size() == 1) {
            String collectionId = apiData.getCollections().keySet().iterator().next();
            FeatureTypeConfigurationOgcApi featureTypeConfiguration = apiData.getCollections()
                                                                             .get(collectionId);
            distributionLinks.addAll(featureTypeConfiguration.getAdditionalLinks()
                                                             .stream()
                                                             .filter(link -> Objects.equals(link.getRel(), "enclosure"))
                                                             .collect(Collectors.toUnmodifiableList()));

            ImmutableOgcApiCollection.Builder ogcApiCollection = ImmutableOgcApiCollection.builder()
                                                                                          .id(collectionId);
            for (CollectionExtension ogcApiCollectionExtension : extensionRegistry.getExtensionsForType(CollectionExtension.class)) {
                ogcApiCollection = ogcApiCollectionExtension.process(ogcApiCollection,
                                                                     featureTypeConfiguration,
                                                                     apiData,
                                                                     uriCustomizer.copy()
                                                                                  .clearParameters()
                                                                                  .ensureLastPathSegments("collections", collectionId)
                                                                                  .ensureNoTrailingSlash(),
                                                                     false,
                                                                     mediaType,
                                                                     alternateMediaTypes,
                                                                     language);
            }
            distributionLinks.addAll(ogcApiCollection.build()
                                                     .getLinks()
                                                     .stream()
                                                     .filter(link -> Objects.equals(link.getRel(), "items") && !Objects.equals(link.getType(), "text/html"))
                                                     .collect(Collectors.toUnmodifiableList()));
        }

        landingPageBuilder.putExtensions("datasetDownloadLinks", distributionLinks.build());

        return landingPageBuilder;
    }
}
