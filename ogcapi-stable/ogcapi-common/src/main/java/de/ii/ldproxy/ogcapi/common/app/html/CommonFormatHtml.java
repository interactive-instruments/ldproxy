/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.common.domain.LandingPage;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Component
@Provides
@Instantiate
public class CommonFormatHtml implements CommonFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    @Requires
    private I18n i18n;

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
                        .removeLastPathSegments(api.getData().getSubPathLength())
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
                                                     .removeLastPathSegments(api.getData().getSubPathLength() + 1)
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
