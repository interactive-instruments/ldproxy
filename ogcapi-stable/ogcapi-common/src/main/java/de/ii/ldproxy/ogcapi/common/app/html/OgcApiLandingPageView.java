/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.html;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.common.domain.LandingPage;
import de.ii.ldproxy.ogcapi.domain.ExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.mayThrow;

public class OgcApiLandingPageView extends OgcApiView {
    private final LandingPage apiLandingPage;
    public final String mainLinksTitle;
    public final String apiInformationTitle;
    public Optional<String> catalogUrl;
    public String dataSourceUrl;
    public String keywords;
    public String keywordsWithQuotes;
    public Metadata metadata;
    public URICustomizer uriCustomizer;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public TemporalExtent temporalExtent;
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
    public String externalDocsTitle;
    public String none;
    public boolean isLandingPage = true;

    public OgcApiLandingPageView(OgcApiDataV2 apiData, LandingPage apiLandingPage,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfiguration htmlConfig,
                                 boolean noIndex, URICustomizer uriCustomizer, I18n i18n, Optional<Locale> language) {
        super("landingPage.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                apiLandingPage.getLinks(),
                apiLandingPage.getTitle()
                              .orElse(apiData.getId())
                              .replace("\"", "\\\""),
                apiLandingPage.getDescription()
                              .orElse("")
                              .replace("\"", "\\\""));
        this.apiLandingPage = apiLandingPage;
        this.uriCustomizer = uriCustomizer;

        BoundingBox spatialExtent = apiData.getSpatialExtent().orElse(null);
        this.bbox2 = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.getXmin()),
                "minLat", Double.toString(spatialExtent.getYmin()),
                "maxLng", Double.toString(spatialExtent.getXmax()),
                "maxLat", Double.toString(spatialExtent.getYmax()));
        this.temporalExtent = apiData.getTemporalExtent().orElse(null);
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

        catalogUrl = links.stream()
                          .filter(link -> Objects.equals(link.getRel(), "self"))
                          .map(Link::getHref)
                          .map(mayThrow(url -> new URICustomizer(url)
                                  .clearParameters()
                                  .removeLastPathSegments(apiData.getApiVersion().isPresent()? 2 : 1)
                                  .ensureNoTrailingSlash()
                                  .toString()))
                          .findFirst();

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
        this.apiInformationTitle = i18n.get ("apiInformationTitle", language);
        this.mainLinksTitle = i18n.get ("mainLinksTitle", language);
        this.externalDocsTitle = i18n.get ("externalDocsTitle", language);
        this.none = i18n.get ("none", language);
    }

    public List<Link> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|data|tiles|styles|service-desc|service-doc|ogc-dapa|ldp-map)$"))
                .collect(Collectors.toList());
    }

    public Optional<Link> getData() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "data"))
                .findFirst();
    }

    public Optional<Link> getTiles() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "tiles"))
                .findFirst();
    }

    public Optional<Link> getStyles() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "styles"))
                .findFirst();
    }

    public Optional<Link> getDapa() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "ogc-dapa"))
                .findFirst();
    }

    public Optional<Link> getMap() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "ldp-map"))
                .findFirst();
    }

    public Optional<Link> getApiDefinition() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "service-desc"))
                .findFirst();
    }

    public Optional<Link> getApiDocumentation() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "service-doc"))
                .findFirst();
    }

    public List<Link> getDistributions() {
        return apiData.getCollections()
                .values()
                .stream()
                .filter(featureType -> apiData.isCollectionEnabled(featureType.getId()))
                .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                .map(featureType -> new ImmutableLink.Builder()
                        .title(featureType.getLabel())
                        .href(uriCustomizer.removeParameters().ensureLastPathSegments("collections",featureType.getId(),"items").toString())
                        .type("application/geo+json") // TODO: determine from extensions
                        .rel("start")
                        .build())
                .collect(Collectors.toList());
    }

    public Optional<ExternalDocumentation> getExternalDocs() {
        return apiLandingPage.getExternalDocs();
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
                .map(Link::getHref)
                .map(mayThrow(url -> new URICustomizer(url)
                        .clearParameters()
                        .ensureNoTrailingSlash()
                        .toString()))
                .findFirst()
                .orElse(null);
    }
}
