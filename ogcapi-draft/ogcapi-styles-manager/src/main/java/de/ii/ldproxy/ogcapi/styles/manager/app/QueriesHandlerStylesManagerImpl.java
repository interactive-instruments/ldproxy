/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.manager.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.json.domain.JsonConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import de.ii.ldproxy.ogcapi.styles.manager.domain.QueriesHandlerStylesManager;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.swing.text.Style;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Instantiate
@Provides
public class QueriesHandlerStylesManagerImpl implements QueriesHandlerStylesManager {

    private final I18n i18n;
    private final StyleRepository styleRepository;
    private final ExtensionRegistry extensionRegistry;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    public QueriesHandlerStylesManagerImpl(@Requires ExtensionRegistry extensionRegistry,
                                           @Requires I18n i18n,
                                           @Requires StyleRepository styleRepository) {
        this.extensionRegistry = extensionRegistry;
        this.i18n = i18n;
        this.styleRepository = styleRepository;
        this.queryHandlers = ImmutableMap.of(
                Query.CREATE_STYLE, QueryHandler.with(QueryInputStyleCreateReplace.class, this::createOrReplaceStyle),
                Query.REPLACE_STYLE, QueryHandler.with(QueryInputStyleCreateReplace.class, this::createOrReplaceStyle),
                Query.DELETE_STYLE, QueryHandler.with(QueryInputStyleDelete.class, this::deleteStyle),
                Query.REPLACE_STYLE_METADATA, QueryHandler.with(QueryInputStyleMetadata.class, this::replaceStyleMetadata),
                Query.UPDATE_STYLE_METADATA, QueryHandler.with(QueryInputStyleMetadata.class, this::updateStyleMetadata)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response createOrReplaceStyle(QueryInputStyleCreateReplace queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();
        Optional<String> optionalStyleId = queryInput.getStyleId();
        boolean strict = queryInput.getStrict();
        boolean dryRun = queryInput.getDryRun();
        MediaType contentType = queryInput.getContentType();
        byte[] requestBody = queryInput.getRequestBody();

        StyleFormatExtension format = styleRepository.getStyleFormatStream(apiData, collectionId)
                                                     .filter(f -> contentType.isCompatible(f.getMediaType().type()))
                                                     .filter(f -> f.canSupportTransactions())
                                                     .findAny()
                                                     .orElseThrow(() -> new WebApplicationException(String.format("The content type '%s' is not supported for styles.", contentType.getType()), Response.Status.UNSUPPORTED_MEDIA_TYPE));

        // PUT: check that the style does exist (including as a derived style)
        if (optionalStyleId.isPresent() && !styleRepository.getStyleIds(apiData, collectionId, true)
                                                           .contains(optionalStyleId.get()))
            throw new NotFoundException(String.format("A style with the identifier '%s' does not exist.", optionalStyleId.get()));

        // Validate stylesheet and, if supported, derive the id of the style
        Optional<String> optionalId = format.analyze(new StylesheetContent(requestBody, "[request body]", false), strict);

        boolean useIdFromStylesheet= optionalId.isPresent() && apiData.getExtension(StylesConfiguration.class)
                                                                      .map(cfg -> cfg.getUseIdFromStylesheet())
                                                                      .orElse(false);

        String styleId;
        if (useIdFromStylesheet) {
            if (optionalStyleId.isPresent()) {
                // PUT: the id is always taken from the path, ignore the id derived from the stylesheet
                styleId = optionalStyleId.get();
            } else {
                // POST: throw an exception, if the style id already exists as a style - excluding derived styles
                if (styleRepository.getStyleIds(apiData, collectionId, false).contains(optionalId.get()))
                    throw new WebApplicationException(String.format("A style with the identifier '%s' already exists. Please use another identifier in the stylesheet."), Response.Status.CONFLICT);
                styleId = optionalId.get();
            }
        } else {
            if (optionalStyleId.isEmpty()) {
                // POST: use the next available id
                styleId = styleRepository.getNewStyleId(apiData, collectionId);
            } else {
                // PUT: the id is always taken from the path
                styleId = optionalStyleId.get();
            }
        }

        if (dryRun)
            return Response.noContent()
                           .build();

        try {
            styleRepository.writeStyleDocument(apiData, collectionId, styleId, format, requestBody);
        } catch (Exception e) {
            // something went wrong, try to clean up
            try {
                styleRepository.deleteStyle(apiData, collectionId, styleId);
            } catch (IOException ioException) {
                // nothing to do
            }
            throw new WebApplicationException("Could not write the style to the store.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (optionalStyleId.isEmpty()) {
            // POST
            // Return 201 with Location header
            URI newURI;
            try {
                newURI = requestContext.getUriCustomizer()
                                       .copy()
                                       .clearParameters()
                                       .ensureLastPathSegment(styleId)
                                       .build();
            } catch (URISyntaxException e) {
                throw new WebApplicationException("Could not determine URI for the new style.", e, Response.Status.INTERNAL_SERVER_ERROR);
            }

            return Response.created(newURI)
                           .build();
        }

        // PUT
        return Response.noContent()
                       .build();
    }

    private Response deleteStyle(QueryInputStyleDelete queryInput, ApiRequestContext requestContext) {

        try {
            styleRepository.deleteStyle(requestContext.getApi().getData(),
                                        queryInput.getCollectionId(),
                                        queryInput.getStyleId());
        } catch (IOException e) {
            throw new WebApplicationException("Could not delete the style from the store.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent()
                       .build();
    }

    private Response replaceStyleMetadata(QueryInputStyleMetadata queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();
        String styleId = queryInput.getStyleId();
        boolean strict = queryInput.getStrict();
        boolean dryRun = queryInput.getDryRun();
        MediaType contentType = queryInput.getContentType();
        byte[] requestBody = queryInput.getRequestBody();

        StyleMetadataFormatExtension format = styleRepository.getStyleMetadataFormatStream(apiData, collectionId)
                                                             .filter(f -> contentType.isCompatible(f.getMediaType().type()))
                                                             .filter(f -> f.canSupportTransactions())
                                                             .findAny()
                                                             .orElseThrow(() -> new WebApplicationException(String.format("The content type '%s' is not supported for style metadata.", contentType.getType()), Response.Status.UNSUPPORTED_MEDIA_TYPE));

        // check that the style does exist (including as a derived style)
        if (!styleRepository.getStyleIds(apiData, collectionId, true)
                            .contains(styleId))
            throw new NotFoundException(String.format("A style with the identifier '%s' does not exist.", styleId));

        // Validate style metadata by parsing it
        format.parse(requestBody, strict, false);

        if (dryRun)
            return Response.noContent()
                           .build();

        try {
            styleRepository.writeStyleMetadataDocument(apiData, collectionId, styleId, requestBody);
        } catch (Exception e) {
            throw new WebApplicationException("Could not write the style metadata to the store.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent()
                       .build();
    }

    private Response updateStyleMetadata(QueryInputStyleMetadata queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();
        String styleId = queryInput.getStyleId();
        boolean strict = queryInput.getStrict();
        boolean dryRun = queryInput.getDryRun();
        MediaType contentType = queryInput.getContentType();
        byte[] requestBody = queryInput.getRequestBody();

        StyleMetadataFormatExtension format = styleRepository.getStyleMetadataFormatStream(apiData, collectionId)
                                                             .filter(f -> contentType.isCompatible(f.getMediaType().type()))
                                                             .filter(f -> f.canSupportTransactions())
                                                             .findAny()
                                                             .orElseThrow(() -> new WebApplicationException(String.format("The content type '%s' is not supported for style metadata.", contentType.getType()), Response.Status.UNSUPPORTED_MEDIA_TYPE));

        // check that the style does exist (including as a derived style)
        if (!styleRepository.getStyleIds(apiData, collectionId, true)
                            .contains(styleId))
            throw new NotFoundException(String.format("A style with the identifier '%s' does not exist.", styleId));

        byte[] patched = styleRepository.updateStyleMetadataPatch(apiData, Optional.empty(), styleId, requestBody, strict);

        if (dryRun)
            return Response.noContent()
                           .build();

        try {
            styleRepository.writeStyleMetadataDocument(apiData, collectionId, styleId, patched);
        } catch (Exception e) {
            throw new WebApplicationException("Could not write the style metadata to the store.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent()
                       .build();
    }
}
