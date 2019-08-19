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
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Collections;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3DatasetMetadataExtension;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Collection;
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

    @Requires
    private KeyValueStore keyValueStore;

    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public ImmutableWfs3Collections.Builder process(ImmutableWfs3Collections.Builder collections, URICustomizer uriCustomizer, Collection<FeatureTypeConfigurationWfs3> featureTypeConfigurationsWfs3, Wfs3ServiceData serviceData) {
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        List<Wfs3Link> wfs3Links = stylesLinkGenerator.generateLandingPageLinks(uriCustomizer);
        collections.addLinks(wfs3Links.get(0));

        List<String> styleDocumentList = keyValueStore.getChildStore("styles")
                                               .getChildStore(serviceData.getId())
                                               .getKeys();

        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(serviceData, StylesConfiguration.class);

        // TODO: review maps
        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getMapsEnabled()) {
            ImmutableList.Builder<Map<String, String>> mapLinks = ImmutableList.builder();

            for (String styleDoc : styleDocumentList) {
                String styleId = styleDoc.substring(0,styleDoc.lastIndexOf("."));
                mapLinks.add(ImmutableMap.of("title", styleId,
                        "url", uriCustomizer.ensureLastPathSegments("maps", styleId)
                                                .toString(),
                        "target", "_blank"));
            }

            collections.addSections(ImmutableMap.of("title", "Maps", "links", mapLinks.build()));
        }

        return collections;
    }
}
