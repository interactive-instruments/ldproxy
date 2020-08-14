/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.features.processing.Processing;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormatProcessing;
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
public class ObservationProcessingProcessingOutputFormatHtml implements ObservationProcessingOutputFormatProcessing {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    @Requires
    private I18n i18n;

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, HtmlConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), HtmlConfiguration.class);
    }

    private boolean isNoIndexEnabledForApi(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Object getEntity(Processing processing,
                            String collectionId,
                            OgcApiApi api,
                            OgcApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());
        String processingTitle = i18n.get("processingTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(4)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(3)
                                .toString()))
                .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(api.getData().getCollections().get(collectionId).getLabel(), requestContext.getUriCustomizer().copy()
                                                                                                                  .removeLastPathSegments(1)
                                                                                                                  .toString()))
                .add(new NavigationDTO(processingTitle))
                .build();

        HtmlConfiguration htmlConfiguration = api.getData()
                                                 .getCollections()
                                                 .get(collectionId)
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        return new ObservationProcessingProcessingView(api.getData(), processing, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfiguration, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }

}
