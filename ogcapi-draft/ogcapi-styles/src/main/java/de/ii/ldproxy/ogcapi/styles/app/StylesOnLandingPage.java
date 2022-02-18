/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesLinkGenerator;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * add styles information to the landing page
 *
 */
@Component
@Provides
@Instantiate
public class StylesOnLandingPage implements LandingPageExtension {

    private final I18n i18n;
    private final StyleRepository styleRepo;

    public StylesOnLandingPage(@Requires I18n i18n,
                               @Requires StyleRepository styleRepo) throws IOException {
        this.i18n = i18n;
        this.styleRepo = styleRepo;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder,
                                                OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        String defaultStyle = apiData.getExtension(StylesConfiguration.class)
                                     .map(StylesConfiguration::getDefaultStyle)
                                     .map(s -> s.equals("NONE") ? null : s)
                                     .orElse(null);
        if (Objects.isNull(defaultStyle)) {
            defaultStyle = apiData.getExtension(HtmlConfiguration.class)
                                  .map(HtmlConfiguration::getDefaultStyle)
                                  .map(s -> s.equals("NONE") ? null : s)
                                  .orElse(null);
        }
        if (Objects.nonNull(defaultStyle)) {
            Optional<StyleFormatExtension> htmlStyleFormat = styleRepo.getStyleFormatStream(apiData, Optional.empty()).filter(f -> f.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)).findAny();
            if (htmlStyleFormat.isPresent() && !styleRepo.stylesheetExists(apiData, Optional.empty(), defaultStyle, htmlStyleFormat.get(), true))
                defaultStyle = null;
        }
        List<Link> links = stylesLinkGenerator.generateLandingPageLinks(uriCustomizer, Optional.ofNullable(defaultStyle), i18n, language);
        landingPageBuilder.addAllLinks(links);

        return landingPageBuilder;
    }
}
