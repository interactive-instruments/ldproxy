/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.API_RESOURCES_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ogcapi.common.domain.LandingPageExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.resources.domain.ResourcesConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * add resources link to the landing page
 *
 */
@Singleton
@AutoBind
public class ResourcesOnLandingPage implements LandingPageExtension {

    private final I18n i18n;
    private final Path resourcesStore;

    @Inject
    public ResourcesOnLandingPage(AppContext appContext,
                                  I18n i18n)  {
        this.resourcesStore = appContext.getDataDir()
            .resolve(API_RESOURCES_DIR)
            .resolve("resources");
        this.i18n = i18n;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
            .mode(apiValidation);

        try {
            Files.createDirectories(resourcesStore);
        } catch (IOException e) {
           builder.addErrors();
        }

        return builder.build();
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return LandingPageExtension.super.isEnabledForApi(apiData) ||
            apiData.getExtension(StylesConfiguration.class).map(StylesConfiguration::isResourcesEnabled).orElse(false);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ResourcesConfiguration.class;
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

        final ResourcesLinkGenerator linkGenerator = new ResourcesLinkGenerator();

        List<Link> links = linkGenerator.generateLandingPageLinks(uriCustomizer, i18n, language);
        landingPageBuilder.addAllLinks(links);

        final String datasetId = apiData.getId();
        File apiDir = new File(resourcesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        return landingPageBuilder;
    }
}
