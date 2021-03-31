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
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.LandingPage;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiDatasetView;
import de.ii.ldproxy.ogcapi.domain.ExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class OgcApiLandingPageView extends OgcApiDatasetView {

    private final LandingPage apiLandingPage;
    public final String mainLinksTitle;
    public final String apiInformationTitle;
    public List<Link> distributionLinks;
    public String dataSourceUrl;
    public String keywords;
    public String keywordsWithQuotes;
    public Metadata metadata;
    public boolean spatialSearch;
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
    public String attributionTitle;
    public String none;
    public boolean isDataset;

    public OgcApiLandingPageView(OgcApiDataV2 apiData, LandingPage apiLandingPage,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfiguration htmlConfig,
                                 boolean noIndex, URICustomizer uriCustomizer, I18n i18n, Optional<Locale> language) {
        super("landingPage.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                apiLandingPage.getLinks(),
                apiLandingPage.getTitle()
                              .orElse(apiData.getId()),
                apiLandingPage.getDescription()
                              .orElse(null),
                uriCustomizer,
                apiLandingPage.getExtent());
        this.apiLandingPage = apiLandingPage;

        this.spatialSearch = false;
        this.isDataset = Objects.nonNull(htmlConfig) ? htmlConfig.getSchemaOrgEnabled() : false;

        this.keywords = apiData.getMetadata()
                               .map(Metadata::getKeywords)
                               .map(v -> Joiner.on(',')
                                               .skipNulls()
                                               .join(v))
                               .orElse(null);
        distributionLinks = Objects.requireNonNullElse((List<Link>) apiLandingPage.getExtensions()
                                                                                  .get("datasetDownloadLinks"), ImmutableList.of());

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
        this.attributionTitle = i18n.get ("attributionTitle", language);
        this.none = i18n.get ("none", language);
    }

    public List<Link> getDistributionLinks() {
        return distributionLinks;
    };

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
                .filter(link -> Objects.equals(link.getRel(), "ogc-dapa-processes"))
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

    public Optional<ExternalDocumentation> getExternalDocs() {
        return apiLandingPage.getExternalDocs();
    }

    public Optional<String> getSchemaOrgDataset() {
        return Optional.of(getSchemaOrgDataset(apiData, Optional.empty(), uriCustomizer.copy(), false));
    }
}
