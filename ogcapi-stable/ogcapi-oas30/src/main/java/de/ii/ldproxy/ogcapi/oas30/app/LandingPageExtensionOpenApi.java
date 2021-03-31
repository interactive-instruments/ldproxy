/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.oas30.app;

import de.ii.ldproxy.ogcapi.common.domain.ApiDefinitionFormatExtension;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.oas30.domain.Oas30Configuration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class LandingPageExtensionOpenApi implements LandingPageExtension {

    private final I18n i18n;
    private final ExtensionRegistry extensionRegistry;

    public LandingPageExtensionOpenApi(@Requires ExtensionRegistry extensionRegistry, @Requires I18n i18n) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return Oas30Configuration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer, ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes, Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        extensionRegistry.getExtensionsForType(ApiDefinitionFormatExtension.class)
                         .stream()
                         .filter(f -> f.isEnabledForApi(apiData))
                         .filter(f -> f.getRel().isPresent())
                         .forEach(f -> landingPageBuilder.addLinks(new ImmutableLink.Builder()
                                                                           .href(uriCustomizer.copy()
                                                                                              .ensureLastPathSegment("api")
                                                                                              .setParameter("f", f.getMediaType().parameter())
                                                                                              .toString())
                                                                           .rel(f.getRel().get())
                                                                           .type(f.getMediaType().type().toString())
                                                                           .title(i18n.get(f.getRel().get().equals("service-desc") ? "serviceDescLink" : "serviceDocLink",language))
                                                                           .build()));

        return landingPageBuilder;
    }
}
