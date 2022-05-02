/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesFormat;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesQueriesHandler;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesType;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
@AutoBind
public class CollectionPropertiesQueriesHandlerImpl implements CollectionPropertiesQueriesHandler {

    private final I18n i18n;
    private final FeaturesCoreProviders providers;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    @Inject
    public CollectionPropertiesQueriesHandlerImpl(I18n i18n,
                                                  FeaturesCoreProviders providers) {
        this.i18n = i18n;
        this.providers = providers;
        this.queryHandlers = ImmutableMap.of(
                Query.COLLECTION_PROPERTIES, QueryHandler.with(QueryInputCollectionProperties.class, this::getCollectionPropertiesResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void checkCollectionId(OgcApiDataV2 apiData, String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    private Response getCollectionPropertiesResponse(QueryInputCollectionProperties queryInput, ApiRequestContext requestContext) {

        final OgcApi api = requestContext.getApi();
        final OgcApiDataV2 apiData = api.getData();
        final String collectionId = queryInput.getCollectionId();
        final CollectionPropertiesType type = queryInput.getType();
        final JsonSchemaCache schemaCache = queryInput.getSchemaCache();

        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat
                .format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
            .get(collectionId);

        CollectionPropertiesFormat outputFormat = api.getOutputFormat(
                CollectionPropertiesFormat.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/"+type.toString(),
                    Optional.of(collectionId))
            .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        checkCollectionId(api.getData(), collectionId);
        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        Optional<String> schemaUri = links.stream()
            .filter(link -> link.getRel().equals("self"))
            .map(Link::getHref)
            .map(link -> !link.contains("?") ? link : link.substring(0, link.indexOf("?")))
            .findAny();

        FeatureSchema featureSchema = providers.getFeatureSchema(apiData, collectionData)
                                               .orElse(new ImmutableFeatureSchema.Builder().name(collectionId)
                                                                                           .type(SchemaBase.Type.OBJECT)
                                                                                           .build());

        JsonSchemaDocument schema = schemaCache
            .getSchema(featureSchema, apiData, collectionData, schemaUri);

        Date lastModified = getLastModified(queryInput);
        EntityTag etag = !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || apiData.getExtension(HtmlConfiguration.class, collectionId).map(HtmlConfiguration::getSendEtags).orElse(false)
            ? getEtag(schema, JsonSchema.FUNNEL, outputFormat)
            : null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? links : null,
                                      HeaderCaching.of(lastModified, etag, queryInput),
                                      null,
                                      HeaderContentDisposition.of(String.format("%s.%s.%s", collectionId, type.toString(), outputFormat.getMediaType().fileExtension())))
            .entity(outputFormat.getEntity(schema, type, links, collectionId, api, requestContext))
            .build();
    }

}
