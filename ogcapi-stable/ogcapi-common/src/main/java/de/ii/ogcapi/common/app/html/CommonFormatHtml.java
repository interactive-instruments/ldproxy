/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.app.html;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ogcapi.common.domain.LandingPage;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class CommonFormatHtml implements CommonFormatExtension, ConformanceClass {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    private final I18n i18n;

    @Inject
    public CommonFormatHtml(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html");
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage,
                                       OgcApi api,
                                       ApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData()
                                                   .getSubPath()
                                                   .size())
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel()))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiLandingPageView landingPageView = new OgcApiLandingPageView(api.getData(), apiLandingPage, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return landingPageView;
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                       OgcApi api, ApiRequestContext requestContext)  {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String conformanceDeclarationTitle = i18n.get("conformanceDeclarationTitle", requestContext.getLanguage());

        final URICustomizer uriCustomizer = requestContext.getUriCustomizer();
        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                                       uriCustomizer.copy()
                                                     .removeLastPathSegments(api.getData()
                                                                                .getSubPath()
                                                                                .size() + 1)
                                                     .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                                       uriCustomizer.copy()
                                                    .removeLastPathSegments(1)
                                                    .toString()))
                .add(new NavigationDTO(conformanceDeclarationTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiConformanceDeclarationView ogcApiConformanceDeclarationView =
                new OgcApiConformanceDeclarationView(conformanceDeclaration, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), i18n, requestContext.getLanguage());
        return ogcApiConformanceDeclarationView;
    }
}
