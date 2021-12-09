/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.Styles;
import de.ii.ldproxy.ogcapi.styles.domain.StylesFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Instantiate
@Provides
public class QueriesHandlerStylesImpl implements QueriesHandlerStyles {

    private final I18n i18n;
    private final StyleRepository styleRepository;
    private final ExtensionRegistry extensionRegistry;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    public QueriesHandlerStylesImpl(@Requires ExtensionRegistry extensionRegistry, @Requires I18n i18n, @Requires StyleRepository styleRepository) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
        this.styleRepository = styleRepository;
        this.queryHandlers = ImmutableMap.of(
                Query.STYLES, QueryHandler.with(QueryInputStyles.class, this::getStylesResponse),
                Query.STYLE, QueryHandler.with(QueryInputStyle.class, this::getStyleResponse),
                Query.STYLE_METADATA, QueryHandler.with(QueryInputStyle.class, this::getStyleMetadataResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getStylesResponse(QueryInputStyles queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();

        Styles styles = styleRepository.getStyles(apiData, collectionId, requestContext, true);

        StylesFormatExtension format = styleRepository.getStylesFormatStream(apiData, collectionId)
                                                      .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
                                                      .findAny()
                                                      .orElseThrow(() -> new NotAcceptableException(
                                                              MessageFormat.format("The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                                                                                   requestContext.getMediaType(),
                                                                                   String.join(", ", styleRepository.getStylesFormatStream(apiData, collectionId).map(f -> f.getMediaType().type().toString()).collect(Collectors.toUnmodifiableList())))));

        Date lastModified = styles.getLastModified()
                                  .orElse(Date.from(Instant.now()));
        EntityTag etag = !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || (collectionId.isEmpty() ? apiData.getExtension(HtmlConfiguration.class) : apiData.getExtension(HtmlConfiguration.class, collectionId.get()))
            .map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(styles, Styles.FUNNEL, format)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext,
                                      queryInput.getIncludeLinkHeader() ? styles.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("styles.%s", format.getMediaType().fileExtension()))
                .entity(format.getStylesEntity(styles, apiData, collectionId, requestContext))
                .build();
    }

    private Response getStyleResponse(QueryInputStyle queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();
        String styleId = queryInput.getStyleId();

        StyleFormatExtension format = styleRepository.getStyleFormatStream(apiData, collectionId)
                                                     .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
                                                     .findAny()
                                                     .orElseThrow(() -> new NotAcceptableException(
                                                             MessageFormat.format("The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                                                                                  requestContext.getMediaType(),
                                                                                  String.join(", ", styleRepository.getStyleFormatStream(apiData, collectionId).map(f -> f.getMediaType().type().toString()).collect(Collectors.toUnmodifiableList())))));

        StylesheetContent stylesheetContent = styleRepository.getStylesheet(apiData, collectionId, styleId, format, requestContext, true);

        // collect self/alternate links, but only, if we need to return them in the headers
        List<Link> links = null;
        if (queryInput.getIncludeLinkHeader()) {
            final DefaultLinksGenerator defaultLinkGenerator = new DefaultLinksGenerator();
            List<ApiMediaType> alternateMediaTypes = styleRepository.getStylesheetMediaTypes(apiData, collectionId, styleId);
            links = defaultLinkGenerator.generateLinks(requestContext.getUriCustomizer(), format.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());
        }

        Date lastModified = styleRepository.getStylesheetLastModified(apiData, collectionId, styleId, format, true);
        EntityTag etag = getEtag(stylesheetContent.getContent());
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, links,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("%s.%s", styleId, format.getFileExtension()))
                .entity(format.getStyleEntity(stylesheetContent, apiData, collectionId, styleId, requestContext))
                .build();
    }

    private Response getStyleMetadataResponse(QueryInputStyle queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();
        StyleMetadata metadata = styleRepository.getStyleMetadata(apiData, collectionId, queryInput.getStyleId(), requestContext);

        StyleMetadataFormatExtension format = styleRepository.getStyleMetadataFormatStream(apiData, collectionId)
                                                             .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
                                                             .findAny()
                                                             .orElseThrow(() -> new NotAcceptableException(
                                                                     MessageFormat.format("The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                                                                                          requestContext.getMediaType(),
                                                                                          String.join(", ", styleRepository.getStyleMetadataFormatStream(apiData, collectionId).map(f -> f.getMediaType().type().toString()).collect(Collectors.toUnmodifiableList())))));

        Date lastModified = styleRepository.getStyleLastModified(apiData, collectionId, queryInput.getStyleId());
        EntityTag etag = !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || (collectionId.isEmpty() ? apiData.getExtension(HtmlConfiguration.class) : apiData.getExtension(HtmlConfiguration.class, collectionId.get()))
            .map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(metadata, StyleMetadata.FUNNEL, format)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? metadata.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("%s.metadata.%s", queryInput.getStyleId(), format.getMediaType().fileExtension()))
                .entity(format.getStyleMetadataEntity(metadata, apiData, collectionId, requestContext))
                .build();
    }
}
