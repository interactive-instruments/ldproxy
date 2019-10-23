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
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import io.dropwizard.views.View;

import java.util.*;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.api.functional.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
public class OgcApiLandingPageView extends View {
    private final LandingPage apiLandingPage;
    private final List<NavigationDTO> breadCrumbs;
    public String urlPrefix;
    public final HtmlConfig htmlConfig;
    public String title;
    public String description;
    public String dataSourceUrl;
    public String keywords;
    public Metadata metadata;
    public List<OgcApiLink> links;
    public URICustomizer uriCustomizer;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public Map<String, String> temporalExtent;
    private final OgcApiDatasetData datasetData;
    public String dataTitle;
    public String apiDefinitionTitle;
    public String apiDocumentationTitle;
    public String providerTitle;
    public String licenseTitle;
    public String spatialExtentTitle;
    public String temporalExtentTitle;
    public String dataSourceTitle;
    public String additionalLinksTitle;
    public String expertInformationTitle;
    public String none;

    public OgcApiLandingPageView(OgcApiDatasetData datasetData, LandingPage apiLandingPage,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig,
                                 URICustomizer uriCustomizer, I18n i18n, Optional<Locale> language) {
        super("landingPage.mustache", Charsets.UTF_8);
        this.apiLandingPage = apiLandingPage;
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;
        this.uriCustomizer = uriCustomizer;

        this.title = apiLandingPage.getTitle()
                .orElse(datasetData.getId());
        this.description = apiLandingPage.getDescription()
                .orElse("");
        this.links = apiLandingPage.getLinks();
        double[] spatialExtent = datasetData.getFeatureTypes()
                .values()
                .stream()
                .map(featureTypeConfigurationWfs3 -> featureTypeConfigurationWfs3.getExtent()
                        .getSpatial()
                        .getCoords())
                .reduce((doubles, doubles2) -> new double[]{
                        Math.min(doubles[0], doubles2[0]),
                        Math.min(doubles[1], doubles2[1]),
                        Math.max(doubles[2], doubles2[2]),
                        Math.max(doubles[3], doubles2[3])})
                .orElse(null);
        this.bbox2 = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent[1]),
                "minLat", Double.toString(spatialExtent[0]),
                "maxLng", Double.toString(spatialExtent[3]),
                "maxLat", Double.toString(spatialExtent[2])); // TODO is axis order mixed up in script.mustache?
        Long[] interval = datasetData.getFeatureTypes()
                .values()
                .stream()
                .map(featureTypeConfiguration -> featureTypeConfiguration.getExtent()
                        .getTemporal())
                .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getComputedEnd()})
                .reduce((longs, longs2) -> new Long[]{
                        Math.min(longs[0], longs2[0]),
                        Math.max(longs[1], longs2[1])})
                .orElse(null);
        this.temporalExtent = interval==null ? null : ImmutableMap.of(
                "start", interval[0].toString(),
                "computedEnd", interval[1].toString());
        this.spatialSearch = false;

        if (Objects.nonNull(datasetData.getMetadata())) {
            this.metadata = datasetData.getMetadata();

            if (!datasetData.getMetadata()
                            .getKeywords()
                            .isEmpty()) {
                this.keywords = Joiner.on(',')
                                      .skipNulls()
                                      .join(datasetData.getMetadata()
                                                       .getKeywords());
            }
        }

        this.datasetData = datasetData;

        this.dataTitle = i18n.get("dataTitle", language);
        this.apiDefinitionTitle = i18n.get("apiDefinitionTitle", language);
        this.apiDocumentationTitle = i18n.get("apiDocumentationTitle", language);
        this.providerTitle = i18n.get("providerTitle", language);
        this.licenseTitle = i18n.get("licenseTitle", language);
        this.spatialExtentTitle = i18n.get("spatialExtentTitle", language);
        this.temporalExtentTitle = i18n.get("temporalExtentTitle", language);
        this.dataSourceTitle = i18n.get("dataSourceTitle", language);
        this.additionalLinksTitle = i18n.get("additionalLinksTitle", language);
        this.expertInformationTitle = i18n.get ("expertInformationTitle", language);
        this.none = i18n.get ("none", language);
    }

    public List<OgcApiLink> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|data|tiles|styles|service-desc|service-doc)$"))
                .collect(Collectors.toList());
    }

    public Optional<OgcApiLink> getData() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "data"))
                .findFirst();
    }

    public Optional<OgcApiLink> getTiles() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "tiles"))
                .findFirst();
    }

    public Optional<OgcApiLink> getStyles() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "styles"))
                .findFirst();
    }

    public Optional<OgcApiLink> getApiDefinition() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "service-desc"))
                .findFirst();
    }

    public Optional<OgcApiLink> getApiDocumentation() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "service-doc"))
                .findFirst();
    }

    public List<OgcApiLink> getDistributions() {
        return datasetData.getFeatureTypes()
                .values()
                .stream()
                .filter(featureType -> datasetData.isFeatureTypeEnabled(featureType.getId()))
                .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                .map(featureType -> new ImmutableOgcApiLink.Builder()
                        .description(featureType.getLabel())
                        .href(uriCustomizer.removeParameters().ensureLastPathSegments("collections",featureType.getId(),"items").toString())
                        .type("application/geo+json") // TODO: determine from extensions
                        .rel("start")
                        .build())
                .collect(Collectors.toList());
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }

    public String getCanonicalUrl() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "self"))
                .map(OgcApiLink::getHref)
                .map(mayThrow(url -> new URICustomizer(url)
                        .clearParameters()
                        .ensureNoTrailingSlash()
                        .toString()))
                .findFirst()
                .orElse(null);
    }

    public Optional<String> getCatalogUrl() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "self"))
                .map(OgcApiLink::getHref)
                .map(mayThrow(url -> new URICustomizer(url)
                        .clearParameters()
                        .removeLastPathSegments(1)
                        .ensureNoTrailingSlash()
                        .toString()))
                .findFirst();
    }

    /* TODO
    public Collections getCollections() {
        ImmutableCollections.Builder builder = new ImmutableCollections.Builder()
                .addAllLinks(collections.getLinks())
                .addAllCrs(collections.getCrs());

        List<Map<String, Object>> collect = collections.getSections()
                                                       .stream()
                                                       .filter(stringObjectMap -> !stringObjectMap.containsKey("collections"))
                                                       .collect(Collectors.toList());

        return builder.addAllSections(collect)
                      .build();
    }

    public List<OgcApiCollectionsView.FeatureType> getFeatureTypes() {
        return getCollectionsStream(collections)
                .map(OgcApiCollectionsView.FeatureType::new)
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

     */
}
