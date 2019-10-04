/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.domain.*;
import io.dropwizard.views.View;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class OgcApiCollectionsView extends View {
    private final List<OgcApiCollection> collections;
    private final List<NavigationDTO> breadCrumbs;
    private final String urlPrefix;
    private final OgcApiDatasetData datasetData;
    public final HtmlConfig htmlConfig;
    public String title;
    public String description;
    public String dataSourceUrl;
    public String keywords;
    public List<String> crs;
    public Metadata metadata;
    public List<OgcApiLink> links;

    public OgcApiCollectionsView(OgcApiDatasetData datasetData, Collections collections,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig) {
        super("collections.mustache", Charsets.UTF_8);
        this.collections = collections.getCollections();
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;

        this.title = collections
                .getTitle()
                .orElse(datasetData.getId());
        this.description = collections
                .getDescription()
                .orElse("");
        this.links = collections
                .getLinks();
        this.crs = collections
                .getCrs();

        if (datasetData.getFeatureProvider()
                       .getDataSourceUrl()
                       .isPresent()) {
            this.dataSourceUrl = datasetData.getFeatureProvider()
                                            .getDataSourceUrl()
                                            .get();
        }

        this.datasetData = datasetData;
    }

    public List<OgcApiLink> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|home)$"))
                .collect(Collectors.toList());
    }

    public List<OgcApiCollection> getCollections() { return collections; }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links
                .stream()
                .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }

    /* TODO

    public String getCanonicalUrl() {
        return links
                  .stream()
                  .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "self"))
                  .map(OgcApiLink::getHref)
                  .map(mayThrow(url -> new URICustomizer(url).clearParameters()
                                                             .ensureTrailingSlash()
                                                             .toString()))
                  .findFirst()
                  .orElse(null);
    }

    public String getCatalogUrl() {
        return links
                  .stream()
                  .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "self"))
                  .map(OgcApiLink::getHref)
                  .map(mayThrow(url -> new URICustomizer(url).clearParameters()
                                                             .removeLastPathSegments(1)
                                                             .ensureTrailingSlash()
                                                             .toString()))
                  .findFirst()
                  .orElse(null);
    }

    public String getApiUrl() {
        return links
                  .stream()
                  .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "service-doc") && Objects.equals(wfs3Link.getType(), OgcApiFeaturesOutputFormatHtml.MEDIA_TYPE.type()
                                                                                                                                                        .toString()))
                  .map(OgcApiLink::getHref)
                  .findFirst()
                  .orElse("");
    }

    public List<FeatureType> getFeatureTypes() {
        return getCollectionsStream(collections)
                .map(FeatureType::new)
                .collect(Collectors.toList());
    }

    public List<Distribution> getDistributions() {
        return getCollectionsStream(collections)
                .flatMap(wfs3Collection -> wfs3Collection.getLinks()
                                                         .stream())
                .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "items") && !Objects.equals(wfs3Link.getType(), OgcApiFeaturesOutputFormatHtml.MEDIA_TYPE.type()
                                                                                                                                                    .toString()))
                .map(wfs3Link -> new Distribution(wfs3Link.getTitle(), wfs3Link.getType(), wfs3Link.getHref()))
                .collect(Collectors.toList());
    }

    private Stream<OgcApiCollection> getCollectionsStream(Collections collections) {
        return collections.getSections()
                      .stream()
                      .filter(stringObjectMap -> stringObjectMap.containsKey("collections"))
                      .flatMap(stringObjectMap -> ((List<OgcApiCollection>) stringObjectMap.get("collections")).stream());
    }

    public Optional<String> getTemporalCoverage() {
        return datasetData.getFeatureTypes()
                          .values()
                          .stream()
                          .map(featureTypeConfigurationWfs3 -> featureTypeConfigurationWfs3.getExtent()
                                                                                           .getTemporal())
                          .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getComputedEnd()})
                          .reduce((longs, longs2) -> new Long[]{Math.min(longs[0], longs2[0]), Math.max(longs[1], longs2[1])})
                          .map(longs -> Interval.of(Instant.ofEpochMilli(longs[0]), Instant.ofEpochMilli(longs[1]))
                                                .toString());
    }

    public Optional<String> getSpatialCoverage() {
        return datasetData.getFeatureTypes()
                          .values()
                          .stream()
                          .map(featureTypeConfigurationWfs3 -> featureTypeConfigurationWfs3.getExtent()
                                                                                           .getSpatial()
                                                                                           .getCoords())
                          .reduce((doubles, doubles2) -> new double[]{Math.min(doubles[0], doubles2[0]), Math.min(doubles[1], doubles2[1]), Math.max(doubles[2], doubles2[2]), Math.max(doubles[3], doubles2[3])})
                          .map(bbox -> String.format(Locale.US, "%f %f %f %f", bbox[1], bbox[0], bbox[3], bbox[2]));
    }

    static class Distribution {
        public final String name;
        public final String encodingFormat;
        public final String url;

        Distribution(String name, String encodingFormat, String url) {
            this.name = name;
            this.encodingFormat = encodingFormat;
            this.url = url;
        }
    }

    static class FeatureType extends OgcApiCollection {
        private final OgcApiCollection collection;

        public FeatureType(OgcApiCollection collection) {
            this.collection = collection;
        }

        public String getUrl() {
            return this.getLinks()
                       .stream()
                       .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "items") && Objects.equals(wfs3Link.getType(), OgcApiFeaturesOutputFormatHtml.MEDIA_TYPE.type()
                                                                                                                                                          .toString()))
                       .map(OgcApiLink::getHref)
                       .findFirst()
                       .orElse("");
        }

        @Override
        public String getId() {
            return collection.getId();
        }

        @Override
        public Optional<String> getTitle() {
            return collection.getTitle();
        }

        @Override
        public Optional<String> getDescription() {
            return collection.getDescription();
        }

        @Override
        public OgcApiExtent getExtent() {
            return collection.getExtent();
        }

        @Override
        public List<OgcApiLink> getLinks() {
            return collection.getLinks();
        }

        @Override
        public List<String> getCrs() {
            return collection.getCrs();
        }

        @Override
        public Optional<String> getItemType() { return Optional.of("Feature"); }

        // TODO
        // @Override
        // public String getPrefixedName() {
        //    return collection.getPrefixedName();
        // }

        @Override
        public Map<String, Object> getExtensions() {
            return collection.getExtensions();
        }
    }


    public String getUrlPrefix() {
        return urlPrefix;
    }
    */
}
