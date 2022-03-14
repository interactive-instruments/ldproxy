/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ogcapi.common.domain.LandingPageExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ogcapi.styles.domain.StyleRepository;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.styles.domain.StylesLinkGenerator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * add styles information to the landing page
 *
 */
@Singleton
@AutoBind
public class StylesOnLandingPage implements LandingPageExtension {

    private final I18n i18n;
    private final StyleRepository styleRepo;

    @Inject
    public StylesOnLandingPage(I18n i18n,
                               StyleRepository styleRepo) {
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
