/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import de.ii.ldproxy.ogcapi.domain.ImmutableDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3DatasetMetadataExtension;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * add styles information to the landing page
 *
 */
@Component
@Provides
@Instantiate
public class StylesOnLandingPage implements Wfs3DatasetMetadataExtension {

    private final StylesStore stylesStore;

    public StylesOnLandingPage(@Requires StylesStore stylesStore) {
        this.stylesStore = stylesStore;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData dataset) {
        return isExtensionEnabled(dataset, StylesConfiguration.class);
    }

    @Override
    public ImmutableDataset.Builder process(ImmutableDataset.Builder datasetBuilder, OgcApiDatasetData datasetData,
                                            URICustomizer uriCustomizer,
                                            OgcApiMediaType mediaType,
                                            List<OgcApiMediaType> alternativeMediaTypes) {

        if (!isEnabledForDataset(datasetData)) {
            return datasetBuilder;
        }

        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        List<Wfs3Link> wfs3Links = stylesLinkGenerator.generateLandingPageLinks(uriCustomizer);
        datasetBuilder.addAllLinks(wfs3Links);

        List<String> styleDocumentList = stylesStore.ids(datasetData.getId());

        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(datasetData, StylesConfiguration.class);

        // TODO: review maps
        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getMapsEnabled()) {
            ImmutableList.Builder<Map<String, String>> mapLinks = ImmutableList.builder();

            for (String styleDoc : styleDocumentList) {
                String styleId = Files.getNameWithoutExtension(styleDoc);
                mapLinks.add(ImmutableMap.of("title", styleId,
                        "url", uriCustomizer.ensureLastPathSegments("maps", styleId)
                                            .toString(),
                        "target", "_blank"));
            }

            datasetBuilder.addSections(ImmutableMap.of("title", "Maps", "links", mapLinks.build()));
        }

        return datasetBuilder;
    }
}
