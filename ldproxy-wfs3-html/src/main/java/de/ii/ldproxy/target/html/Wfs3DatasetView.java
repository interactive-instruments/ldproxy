/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3Extent;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceMetadata;
import io.dropwizard.views.View;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.util.functional.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
public class Wfs3DatasetView extends View {
    private final Wfs3Collections wfs3Dataset;
    private final List<NavigationDTO> breadCrumbs;
    private final String urlPrefix;
    public final HtmlConfig htmlConfig;
    public String title;
    public String description;
    public String dataSourceUrl;
    public String keywords;
    public Wfs3ServiceMetadata metadata;
    private final Wfs3ServiceData serviceData;


    public Wfs3DatasetView(Wfs3ServiceData serviceData, Wfs3Collections wfs3Dataset, final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig) {
        super("service.mustache", Charsets.UTF_8);
        this.wfs3Dataset = wfs3Dataset;
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;


        this.title = serviceData.getLabel();
        this.description = serviceData.getDescription()
                                      .orElse("");

        if (serviceData.getMetadata()
                       .isPresent() && !serviceData.getMetadata()
                                                   .get()
                                                   .getKeywords()
                                                   .isEmpty()) {
            this.keywords = Joiner.on(',')
                                  .skipNulls()
                                  .join(serviceData.getMetadata()
                                                   .get()
                                                   .getKeywords());
        }

        if (serviceData.getFeatureProvider()
                       .getDataSourceUrl()
                       .isPresent()) {
            this.dataSourceUrl = serviceData.getFeatureProvider()
                                            .getDataSourceUrl()
                                            .get();
        }

        if (serviceData.getMetadata()
                       .isPresent()) {
            this.metadata = serviceData.getMetadata()
                                       .get();
        }
        this.serviceData = serviceData;
    }

    public Wfs3Collections getWfs3Dataset() {
        return wfs3Dataset;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public String getCanonicalUrl() {
        return wfs3Dataset.getLinks()
                          .stream()
                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "self"))
                          .map(Wfs3Link::getHref)
                          .map(mayThrow(url -> new URICustomizer(url).clearParameters().ensureTrailingSlash()
                                                                     .toString()))
                          .findFirst()
                          .orElse(null);
    }

    public String getCatalogUrl() {
        return wfs3Dataset.getLinks()
                          .stream()
                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "self"))
                          .map(Wfs3Link::getHref)
                          .map(mayThrow(url -> new URICustomizer(url).clearParameters()
                                                                     .removeLastPathSegments(1).ensureTrailingSlash()
                                                                     .toString()))
                          .findFirst()
                          .orElse(null);
    }

    public String getApiUrl() {
        return wfs3Dataset.getLinks()
                          .stream()
                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "service") && Objects.equals(wfs3Link.getType(), Wfs3OutputFormatHtml.MEDIA_TYPE.main()
                                                                                                                                                                .toString()))
                          .map(Wfs3Link::getHref)
                          .findFirst()
                          .orElse("");
    }

    public List<FeatureType> getFeatureTypes() {
        return wfs3Dataset.getCollections()
                          .stream()
                          .map(FeatureType::new)
                          .collect(Collectors.toList());
    }

    public List<Distribution> getDistributions() {
        return wfs3Dataset.getCollections()
                          .stream()
                          .flatMap(wfs3Collection -> wfs3Collection.getLinks()
                                                                   .stream())
                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "item") && !Objects.equals(wfs3Link.getType(), Wfs3OutputFormatHtml.MEDIA_TYPE.main()
                                                                                                                                                             .toString()))
                          .map(wfs3Link -> new Distribution(wfs3Link.getTitle(), wfs3Link.getType(), wfs3Link.getHref()))
                          .collect(Collectors.toList());
    }

    public List<NavigationDTO> getFormats() {
        return wfs3Dataset.getLinks()
                          .stream()
                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                          .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                   .toUpperCase()))
                          .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                          .collect(Collectors.toList());
    }

    public Optional<String> getTemporalCoverage() {
        return serviceData.getFeatureTypes().values().stream()
                .map(featureTypeConfigurationWfs3 -> featureTypeConfigurationWfs3.getExtent().getTemporal())
                .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getComputedEnd()})
                .reduce((longs, longs2) -> new Long[]{Math.min(longs[0], longs2[0]), Math.max(longs[1], longs2[1])})
                .map(longs -> Interval.of(Instant.ofEpochMilli(longs[0]), Instant.ofEpochMilli(longs[1])).toString());
    }

    public Optional<String> getSpatialCoverage() {
        return serviceData.getFeatureTypes().values().stream()
                          .map(featureTypeConfigurationWfs3 -> featureTypeConfigurationWfs3.getExtent().getSpatial().getCoords())
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

    static class FeatureType extends Wfs3Collection {
        private final Wfs3Collection collection;

        public FeatureType(Wfs3Collection collection) {
            this.collection = collection;
        }

        public String getUrl() {
            return this.getLinks()
                       .stream()
                       .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "item") && Objects.equals(wfs3Link.getType(), Wfs3OutputFormatHtml.MEDIA_TYPE.main()
                                                                                                                                                          .toString()))
                       .map(Wfs3Link::getHref)
                       .findFirst()
                       .orElse("");
        }

        @Override
        public String getName() {
            return collection.getName();
        }

        @Override
        public String getTitle() {
            return collection.getTitle();
        }

        @Override
        public Optional<String> getDescription() {
            return collection.getDescription();
        }

        @Override
        public Wfs3Extent getExtent() {
            return collection.getExtent();
        }

        @Override
        public List<Wfs3Link> getLinks() {
            return collection.getLinks();
        }

        @Override
        public List<String> getCrs() {
            return collection.getCrs();
        }

        @Override
        public String getPrefixedName() {
            return collection.getPrefixedName();
        }

        @Override
        public Map<String, Object> getExtensions() {
            return collection.getExtensions();
        }
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
