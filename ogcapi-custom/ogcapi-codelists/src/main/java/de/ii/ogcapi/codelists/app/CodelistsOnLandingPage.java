/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.codelists.domain.CodelistsConfiguration;
import de.ii.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ogcapi.common.domain.ImmutableLandingPage.Builder;
import de.ii.ogcapi.common.domain.LandingPageExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/** add resources link to the landing page */
@Singleton
@AutoBind
public class CodelistsOnLandingPage implements LandingPageExtension {

  private final I18n i18n;

  @Inject
  public CodelistsOnLandingPage(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return LandingPageExtension.super.isEnabledForApi(apiData);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CodelistsConfiguration.class;
  }

  @Override
  public ImmutableLandingPage.Builder process(
      Builder landingPageBuilder,
      OgcApi api,
      URICustomizer uriCustomizer,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      Optional<Locale> language) {

    if (!isEnabledForApi(api.getData())) {
      return landingPageBuilder;
    }

    final CodelistsLinkGenerator linkGenerator = new CodelistsLinkGenerator();

    List<Link> links = linkGenerator.generateLandingPageLinks(uriCustomizer, i18n, language);
    landingPageBuilder.addAllLinks(links);

    return landingPageBuilder;
  }
}
