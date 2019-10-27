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

import java.util.*;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.api.functional.LambdaWithException.mayThrow;

public class OgcApiLandingPageView extends LdproxyView {
    private final LandingPage apiLandingPage;
    public String dataSourceUrl;
    public String keywords;
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

    public OgcApiLandingPageView(OgcApiDatasetData apiData, LandingPage apiLandingPage,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig,
                                 URICustomizer uriCustomizer, I18n i18n, Optional<Locale> language) {
        super("landingPage.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, urlPrefix,
                apiLandingPage.getLinks(),
                apiLandingPage.getTitle()
                        .orElse(apiData.getId()),
                apiLandingPage.getDescription()
                .orElse(""));
        this.apiLandingPage = apiLandingPage;
        this.uriCustomizer = uriCustomizer;

        double[] spatialExtent = apiData.getFeatureTypes()
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
        Long[] interval = apiData.getFeatureTypes()
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

        if (Objects.nonNull(apiData.getMetadata())) {
            this.metadata = apiData.getMetadata();

            if (!metadata.getKeywords()
                         .isEmpty()) {
                this.keywords = Joiner.on(',')
                                      .skipNulls()
                                      .join(apiData.getMetadata()
                                                       .getKeywords());
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
        return apiData.getFeatureTypes()
                .values()
                .stream()
                .filter(featureType -> apiData.isFeatureTypeEnabled(featureType.getId()))
                .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                .map(featureType -> new ImmutableOgcApiLink.Builder()
                        .title(featureType.getLabel())
                        .href(uriCustomizer.removeParameters().ensureLastPathSegments("collections",featureType.getId(),"items").toString())
                        .type("application/geo+json") // TODO: determine from extensions
                        .rel("start")
                        .build())
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
}
