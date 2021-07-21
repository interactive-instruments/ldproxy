/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeatureGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorGeoJson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Instantiate
@Provides
public class QueryablesQueriesHandlerImpl implements QueryablesQueriesHandler {

    public enum Query implements QueryIdentifier {QUERYABLES}

    @Value.Immutable
    public interface QueryInputQueryables extends QueryInput {
        String getCollectionId();

        boolean getIncludeLinkHeader();
    }

    private final SchemaGeneratorFeatureGeoJson schemaGeneratorFeature;
    private final I18n i18n;
    private final FeaturesCoreProviders providers;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    public QueryablesQueriesHandlerImpl(@Requires I18n i18n,
                                        @Requires FeaturesCoreProviders providers,
                                        @Requires SchemaGeneratorFeatureGeoJson schemaGeneratorFeature) {
        this.i18n = i18n;
        this.providers = providers;
        this.schemaGeneratorFeature = schemaGeneratorFeature;
        this.queryHandlers = ImmutableMap.of(
                Query.QUERYABLES, QueryHandler.with(QueryInputQueryables.class, this::getQueryablesResponse)
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

    // TODO consolidate code
    private Response getQueryablesResponse(QueryInputQueryables queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));

        QueryablesFormatExtension outputFormat = api.getOutputFormat(
                    QueryablesFormatExtension.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/queryables",
                    Optional.of(collectionId))
                                                    .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        checkCollectionId(api.getData(), collectionId);
        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        JsonSchemaObject jsonSchema = schemaGeneratorFeature.getSchemaJson(apiData, collectionId, links.stream()
                                                                                                       .filter(link -> link.getRel().equals("self"))
                                                                                                       .map(link -> link.getHref())
                                                                                                       .map(link -> link.indexOf("?") == -1 ? link : link.substring(0, link.indexOf("?")))
                                                                                                       .findAny(), SchemaGeneratorFeature.SCHEMA_TYPE.QUERYABLES);

        Date lastModified = getLastModified(queryInput, api);
        EntityTag etag = getEtag(jsonSchema, JsonSchema.FUNNEL, outputFormat);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null), null)
                .entity(outputFormat.getEntity(jsonSchema, links, collectionId, api, requestContext))
                .build();
    }
}
