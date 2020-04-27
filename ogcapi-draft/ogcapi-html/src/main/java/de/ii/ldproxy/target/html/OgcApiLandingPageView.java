/**
 * Copyright 2020 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.LandingPage;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.api.functional.LambdaWithException.mayThrow;

public class OgcApiLandingPageView extends OgcApiView {
    private final LandingPage apiLandingPage;
    public String dataSourceUrl;
    public String keywords;
    public String keywordsWithQuotes;
    public Metadata metadata;
    public URICustomizer uriCustomizer;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public Map<String, String> temporalExtent;
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
    public boolean isLandingPage = true;

    public OgcApiLandingPageView(OgcApiApiDataV2 apiData, LandingPage apiLandingPage,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig,
                                 boolean noIndex, URICustomizer uriCustomizer, I18n i18n, Optional<Locale> language) {
        super("landingPage.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                apiLandingPage.getLinks(),
                apiLandingPage.getTitle()
                        .orElse(apiData.getId()),
                apiLandingPage.getDescription()
                .orElse(""));
        this.apiLandingPage = apiLandingPage;
        this.uriCustomizer = uriCustomizer;

        BoundingBox spatialExtent = apiData.getSpatialExtent();
        this.bbox2 = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.getXmin()),
                "minLat", Double.toString(spatialExtent.getYmin()),
                "maxLng", Double.toString(spatialExtent.getXmax()),
                "maxLat", Double.toString(spatialExtent.getYmax()));
        Long[] interval = apiData.getCollections()
                .values()
                .stream()
                .map(FeatureTypeConfigurationOgcApi::getExtent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(FeatureTypeConfigurationOgcApi.CollectionExtent::getTemporal)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getEnd()})
                .reduce((longs, longs2) -> new Long[]{
                        longs[0]==null || longs2[0]==null ? null : Math.min(longs[0], longs2[0]),
                        longs[1]==null || longs2[1]==null ? null : Math.max(longs[1], longs2[1])})
                .orElse(null);
        if (interval==null)
            this.temporalExtent = null;
        else if (interval[0]==null && interval[1]==null)
            this.temporalExtent = ImmutableMap.of();
        else if (interval[0]==null)
            this.temporalExtent = interval==null ? null : ImmutableMap.of(
                "end", interval[1]==null ? null : interval[1].toString());
        else if (interval[1]==null)
            this.temporalExtent = interval==null ? null : ImmutableMap.of(
                "start", interval[0]==null ? null : interval[0].toString());
        else
            this.temporalExtent = interval==null ? null : ImmutableMap.of(
                "start", interval[0]==null ? null : interval[0].toString(),
                "end", interval[1]==null ? null : interval[1].toString());
        this.spatialSearch = false;

        if (apiData.getMetadata().isPresent()) {
            this.metadata = apiData.getMetadata().get();

            if (!metadata.getKeywords()
                         .isEmpty()) {
                this.keywords = Joiner.on(',')
                                      .skipNulls()
                                      .join(metadata.getKeywords());
                this.keywordsWithQuotes = String.join(",", metadata
                        .getKeywords()
                        .stream()
                        .filter(keyword -> Objects.nonNull(keyword) && !keyword.isEmpty())
                        .map(keyword -> ("\"" + keyword + "\""))
                        .collect(Collectors.toList()));
            }
        }

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
        return apiData.getCollections()
                .values()
                .stream()
                .filter(featureType -> apiData.isCollectionEnabled(featureType.getId()))
                .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                .map(featureType -> new ImmutableOgcApiLink.Builder()
                        .title(featureType.getLabel())
                        .href(uriCustomizer.removeParameters().ensureLastPathSegments("collections",featureType.getId(),"items").toString())
                        .type("application/geo+json") // TODO: determine from extensions
                        .rel("start")
                        .build())
                .collect(Collectors.toList());
    }

    public String getDistributionsAsString() {
        return getDistributions()
                .stream()
                .map(link -> "{\"@type\":\"DataDownload\",\"name\":\""+link.getTitle()+"\",\"encodingFormat\":\""+link.getType()+"\",\"contentUrl\":\""+link.getHref()+"\"}")
                .collect(Collectors.joining(","));
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
}
