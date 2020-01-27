/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.Collections;
import de.ii.ldproxy.ogcapi.domain.*;

import java.util.*;
import java.util.stream.Collectors;

public class OgcApiCollectionsView extends LdproxyView {
    private final List<OgcApiCollection> collections;
    public String dataSourceUrl;
    public String keywords;
    public List<String> crs;
    public Metadata metadata;
    public String collectionsTitle;
    public String supportedCrsTitle;
    public String additionalLinksTitle;
    public String expertInformationTitle;
    public String none;
    public String moreInformation;

    public OgcApiCollectionsView(OgcApiApiDataV2 apiData, Collections collections,
                                 final List<NavigationDTO> breadCrumbs, String urlPrefix,
                                 HtmlConfig htmlConfig, boolean noIndex, I18n i18n, Optional<Locale> language, Optional<String> dataSourceUrl) {
        super("collections.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                collections.getLinks(),
                collections
                        .getTitle()
                        .orElse(apiData.getId()),
                collections
                        .getDescription()
                        .orElse("") );
        this.collections = collections.getCollections();
        this.crs = collections
                .getCrs();

        if (dataSourceUrl.isPresent()) {
            this.dataSourceUrl = dataSourceUrl.get();
        }

        this.collectionsTitle = i18n.get("collectionsTitle", language);
        this.supportedCrsTitle = i18n.get("supportedCrsTitle", language);
        this.additionalLinksTitle = i18n.get("additionalLinksTitle", language);
        this.expertInformationTitle = i18n.get ("expertInformationTitle", language);
        this.none = i18n.get ("none", language);
        this.moreInformation = i18n.get("moreInformation", language);
    }

    public List<OgcApiLink> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|home)$"))
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getCollections() {
        return collections.stream()
                .map(collection -> ImmutableMap.of("title", collection.getTitle().orElse(collection.getId()),
                        "id", collection.getId(),
                        "hrefcollection", collection.getLinks()
                                .stream()
                                .filter(link -> link.getRel().equalsIgnoreCase("self"))
                                .findFirst()
                                .map(link -> link.getHref())
                                .orElse(""),
                        "hrefitems", collection.getLinks()
                                .stream()
                                .filter(link -> link.getRel().equalsIgnoreCase("self"))
                                .findFirst()
                                .map(link -> link.getHref() + "/items")
                                .orElse("")))
                .collect(Collectors.toList());
    }
}
