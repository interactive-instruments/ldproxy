/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.util.List;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * add styles information to the landing page
 *
 */
@Component
@Provides
@Instantiate
public class StylesOnLandingPage implements OgcApiLandingPageExtension {

    private final File stylesStore;

    public StylesOnLandingPage(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        this.stylesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styles");
        if (!stylesStore.exists()) {
            stylesStore.mkdirs();
        }
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder,
                                                OgcApiDatasetData apiData,
                                                URICustomizer uriCustomizer,
                                                OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        List<OgcApiLink> ogcApiLinks = stylesLinkGenerator.generateLandingPageLinks(uriCustomizer);
        landingPageBuilder.addAllLinks(ogcApiLinks);

        final String datasetId = apiData.getId();
        File apiDir = new File(stylesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        /* TODO: review maps
        List<String> styleDocumentList = Arrays.stream(apiDir.list()).collect(Collectors.toList());

        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);

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

            landingPageBuilder.addSections(ImmutableMap.of("title", "Maps", "links", mapLinks.build()));
        }
        */

        return landingPageBuilder;
    }
}
