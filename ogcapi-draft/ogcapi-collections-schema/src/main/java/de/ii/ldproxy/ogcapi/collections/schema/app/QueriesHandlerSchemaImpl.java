/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.schema.domain.QueriesHandlerSchema;
import de.ii.ldproxy.ogcapi.collections.schema.domain.SchemaFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaCache;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaDocument;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaDocument.VERSION;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

@Component
@Instantiate
@Provides
public class QueriesHandlerSchemaImpl implements QueriesHandlerSchema {

    public enum Query implements QueryIdentifier {SCHEMA}

    @Value.Immutable
    public interface QueryInputSchema extends QueryInput {
        String getCollectionId();

        boolean getIncludeLinkHeader();

        Optional<String> getProfile();

        String getType();
    }

    private final FeaturesCoreProviders providers;
    private final I18n i18n;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final JsonSchemaCache schemaCache;
    private final JsonSchemaCache schemaCacheCollection;

    public QueriesHandlerSchemaImpl(@Requires I18n i18n, @Requires FeaturesCoreProviders providers, @Requires EntityRegistry entityRegistry) {
        this.i18n = i18n;
        this.providers = providers;
        this.queryHandlers = ImmutableMap.of(
                Query.SCHEMA, QueryHandler.with(QueryInputSchema.class, this::getSchemaResponse)
        );
        this.schemaCache = new SchemaCacheReturnables(() -> entityRegistry.getEntitiesForType(
            Codelist.class));
        this.schemaCacheCollection = new SchemaCacheReturnablesCollection();
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

    private Response getSchemaResponse(QueryInputSchema queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        checkCollectionId(apiData, collectionId);
        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
            .get(collectionId);

        SchemaFormatExtension outputFormat = api.getOutputFormat(SchemaFormatExtension.class,
                                                                 requestContext.getMediaType(),
                                                                 "/collections/"+collectionId+"/schemas/"+queryInput.getType(),
                                                                 Optional.of(collectionId))
                                                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links = new DefaultLinksGenerator()
                .generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        Optional<String> schemaUri = links.stream()
            .filter(link -> Objects.equals(link.getRel(), "self"))
            .map(Link::getHref)
            .findAny();

        FeatureSchema featureSchema = providers.getFeatureSchema(apiData, collectionData)
                                               .orElse(new ImmutableFeatureSchema.Builder().name(collectionId)
                                                                                           .type(SchemaBase.Type.OBJECT)
                                                                                           .build());

        JsonSchemaDocument schema = null;
        if (queryInput.getType().equals("feature"))
            schema = schemaCache.getSchema(featureSchema, apiData, collectionData, schemaUri, getVersion(queryInput.getProfile()));
        else if (queryInput.getType().equals("collection"))
            schema = schemaCacheCollection.getSchema(featureSchema, apiData, collectionData, schemaUri, getVersion(queryInput.getProfile()));

        Date lastModified = getLastModified(queryInput, api);
        EntityTag etag = getEtag(schema, JsonSchema.FUNNEL, outputFormat);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("%s.schema.%s", collectionId, outputFormat.getMediaType().fileExtension()))
                .entity(outputFormat.getEntity(schema, collectionId, api, requestContext))
                .build();
    }

    private VERSION getVersion(Optional<String> profile) {
        if (profile.isEmpty()) {
            return VERSION.current();
        }
        switch (profile.get()) {
            case "2019-09":
                return VERSION.V201909;
            case "07":
                return VERSION.V7;
            default:
                return VERSION.current();
        }
    }
}
