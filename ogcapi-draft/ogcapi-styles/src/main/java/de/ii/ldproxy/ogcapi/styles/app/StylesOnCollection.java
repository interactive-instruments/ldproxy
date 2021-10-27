/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesLinkGenerator;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * add styles information to the collection
 *
 */
@Component
@Provides
@Instantiate
public class StylesOnCollection implements CollectionExtension {

    private final I18n i18n;
    private final StyleRepository styleRepo;

    public StylesOnCollection(@Requires I18n i18n,
                              @Requires StyleRepository styleRepo) throws IOException {
        this.styleRepo = styleRepo;
        this.i18n = i18n;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDataV2 apiData, URICustomizer uriCustomizer, boolean isNested,
                                                     ApiMediaType mediaType, List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        // skip link on /collections
        if (isNested)
            return collection;

        // nothing to add if disabled
        String collectionId = featureTypeConfiguration.getId();
        if (!isEnabledForApi(apiData, collectionId)) {
            return collection;
        }

        String defaultStyle = apiData.getCollections()
                                     .get(collectionId)
                                     .getExtension(StylesConfiguration.class)
                                     .map(StylesConfiguration::getDefaultStyle)
                                     .map(s -> s.equals("NONE") ? null : s)
                                     .orElse(null);
        if (Objects.isNull(defaultStyle)) {
            defaultStyle = apiData.getCollections()
                                  .get(collectionId)
                                  .getExtension(HtmlConfiguration.class)
                                  .map(HtmlConfiguration::getDefaultStyle)
                                  .map(s -> s.equals("NONE") ? null : s)
                                  .orElse(null);
        }
        if (Objects.nonNull(defaultStyle)) {
            Optional<StyleFormatExtension> htmlStyleFormat = styleRepo.getStyleFormatStream(apiData, Optional.of(collectionId)).filter(f -> f.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)).findAny();
            if (htmlStyleFormat.isPresent() && !styleRepo.stylesheetExists(apiData, Optional.of(collectionId), defaultStyle, htmlStyleFormat.get(), true))
                defaultStyle = null;
        }
        return collection.addAllLinks(new StylesLinkGenerator().generateCollectionLinks(uriCustomizer, Optional.ofNullable(defaultStyle), i18n, language));
    }
}
