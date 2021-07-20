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
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeatureCollectionGeoJson;
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
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    private final SchemaGeneratorFeatureGeoJson schemaGeneratorFeature;
    private final SchemaGeneratorFeatureCollectionGeoJson schemaGeneratorFeatureCollection;
    private final I18n i18n;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    public QueriesHandlerSchemaImpl(@Requires I18n i18n,
                                    @Requires SchemaGeneratorFeatureGeoJson schemaGeneratorFeature,
                                    @Requires SchemaGeneratorFeatureCollectionGeoJson schemaGeneratorFeatureCollection) {
        this.i18n = i18n;
        this.schemaGeneratorFeature = schemaGeneratorFeature;
        this.schemaGeneratorFeatureCollection = schemaGeneratorFeatureCollection;
        this.queryHandlers = ImmutableMap.of(
                Query.SCHEMA, QueryHandler.with(QueryInputSchema.class, this::getSchemaResponse)
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

    private Response getSchemaResponse(QueryInputSchema queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        checkCollectionId(apiData, collectionId);

        SchemaFormatExtension outputFormat = api.getOutputFormat(SchemaFormatExtension.class,
                                                                 requestContext.getMediaType(),
                                                                 "/collections/"+collectionId+"/schemas/"+queryInput.getType(),
                                                                 Optional.of(collectionId))
                                                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        Optional<GeoJsonConfiguration> geoJsonConfiguration = apiData.getCollections()
                                                                     .get(collectionId)
                                                                     .getExtension(GeoJsonConfiguration.class);
        boolean flatten = geoJsonConfiguration.filter(geoJsonConfig -> geoJsonConfig.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                                                                       geoJsonConfig.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                              .isPresent();
        SchemaGeneratorFeature.SCHEMA_TYPE schemaType = flatten ? SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT : SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES;

        JsonSchemaObject jsonSchema = queryInput.getType().equals("feature")
                ? schemaGeneratorFeature.getSchemaJson(apiData, collectionId, links.stream()
                                                                                   .filter(link -> link.getRel().equals("self"))
                                                                                   .map(link -> link.getHref())
                                                                                   .findAny(), schemaType, getVersion(queryInput.getProfile()))
                : schemaGeneratorFeatureCollection.getSchemaJson(apiData, collectionId, links.stream()
                                                                                             .filter(link -> link.getRel().equals("self"))
                                                                                             .map(link -> link.getHref())
                                                                                             .findAny(), schemaType, getVersion(queryInput.getProfile()));

        Date lastModified = getLastModified(queryInput, api);
        EntityTag etag = getEtag(jsonSchema, JsonSchema.FUNNEL, outputFormat);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null), null)
                .entity(outputFormat.getEntity(jsonSchema, collectionId, api, requestContext))
                .build();
    }

    private Optional<SchemaGeneratorGeoJson.VERSION> getVersion(Optional<String> profile) {
        if (profile.isEmpty())
            return Optional.empty();
        else if (profile.get().equals("2019-09"))
            return Optional.of(SchemaGeneratorGeoJson.VERSION.V201909);
        else if (profile.get().equals("07"))
            return Optional.of(SchemaGeneratorGeoJson.VERSION.V7);

        return Optional.empty();
    }
}
