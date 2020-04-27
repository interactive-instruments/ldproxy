/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.wfs3.styles.StyleMetadata;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StyleMetadataView extends OgcApiView {
    public StyleMetadata metadata;
    public String none;
    public String metadataTitle;
    public String specificationTitle;
    public String versionTitle;
    public String pointOfContactTitle;
    public String datesTitle;
    public String creationTitle;
    public String publicationTitle;
    public String revisionTitle;
    public String validTillTitle;
    public String receivedOnTitle;
    public String stylesheetsTitle;
    public String nativeTitle;
    public String trueTitle;
    public String falseTitle;
    public String layersTitle;
    public String idTitle;
    public String keywordsTitle;
    public String additionalLinksTitle;
    public String layerTypeTitle;
    public String dataTypeTitle;
    public String attributesTitle;
    public String sampleDataTitle;
    public String requiredTitle;
    public String mediaTypesTitle;
    public String thumbnailTitle;

    public StyleMetadataView(OgcApiApiDataV2 apiData,
                             StyleMetadata metadata,
                             List<NavigationDTO> breadCrumbs,
                             String staticUrlPrefix,
                             HtmlConfig htmlConfig,
                             boolean noIndex,
                             URICustomizer uriCustomizer,
                             I18n i18n,
                             Optional<Locale> language) {
        super("styleMetadata.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                metadata.getLinks(),
                null,
                null);

        // TODO the view could be improved

        this.metadata = metadata;

        this.none = i18n.get ("none", language);

        this.metadataTitle = i18n.get ("metadataTitle", language);
        this.idTitle = i18n.get ("idTitle", language);
        this.keywordsTitle = i18n.get ("keywordsTitle", language);
        this.versionTitle = i18n.get ("versionTitle", language);
        this.pointOfContactTitle = i18n.get ("pointOfContactTitle", language);
        this.datesTitle = i18n.get ("datesTitle", language);
        this.creationTitle = i18n.get ("creationTitle", language);
        this.publicationTitle = i18n.get ("publicationTitle", language);
        this.revisionTitle = i18n.get ("revisionTitle", language);
        this.validTillTitle = i18n.get ("validTillTitle", language);
        this.receivedOnTitle = i18n.get ("receivedOnTitle", language);
        this.stylesheetsTitle = i18n.get ("stylesheetsTitle", language);
        this.specificationTitle = i18n.get ("specificationTitle", language);
        this.nativeTitle = i18n.get ("nativeTitle", language);
        this.trueTitle = i18n.get ("trueTitle", language);
        this.falseTitle = i18n.get ("falseTitle", language);
        this.layersTitle = i18n.get ("layersTitle", language);
        this.additionalLinksTitle = i18n.get ("additionalLinksTitle", language);
        this.layerTypeTitle = i18n.get ("layerTypeTitle", language);
        this.dataTypeTitle = i18n.get ("dataTypeTitle", language);
        this.attributesTitle = i18n.get ("attributesTitle", language);
        this.sampleDataTitle = i18n.get ("sampleDataTitle", language);
        this.requiredTitle = i18n.get ("requiredTitle", language);
        this.mediaTypesTitle = i18n.get ("mediaTypesTitle", language);
        this.thumbnailTitle = i18n.get ("thumbnailTitle", language);
    }

    public OgcApiLink getThumbnail() {
        return links.stream()
                .filter(link -> Objects.equals(link.getRel(), "preview"))
                .findFirst()
                .orElse(null);
    }

    public List<OgcApiLink> getAdditionalLinks() {
        return links.stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|preview|home)$"))
                .collect(Collectors.toList());
    }
}
