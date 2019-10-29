/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * add styles information to the landing page
 *
 */
@Component
@Provides
@Instantiate
public class StylesOnLandingPage implements OgcApiLandingPageExtension {

    @Requires
    I18n i18n;

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
                                                List<OgcApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        List<OgcApiLink> ogcApiLinks = stylesLinkGenerator.generateLandingPageLinks(uriCustomizer, i18n, language);
        landingPageBuilder.addAllLinks(ogcApiLinks);

        final String datasetId = apiData.getId();
        File apiDir = new File(stylesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        return landingPageBuilder;
    }
}
