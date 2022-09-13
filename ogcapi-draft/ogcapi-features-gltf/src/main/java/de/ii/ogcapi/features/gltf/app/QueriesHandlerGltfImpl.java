/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.features.gltf.domain.GltfSchema;
import de.ii.ogcapi.features.gltf.domain.QueriesHandlerGltf;
import de.ii.ogcapi.features.gltf.domain.SchemaFormat3dMetadata;
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
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.web.domain.ETag;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class QueriesHandlerGltfImpl implements QueriesHandlerGltf {

  private final FeaturesCoreProviders providers;
  private final I18n i18n;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final Metadata3dSchemaCacheImpl schemaCache;
  private final EntityRegistry entityRegistry;

  @Inject
  public QueriesHandlerGltfImpl(
      I18n i18n, FeaturesCoreProviders providers, EntityRegistry entityRegistry) {
    this.i18n = i18n;
    this.providers = providers;
    this.entityRegistry = entityRegistry;
    this.queryHandlers =
        ImmutableMap.of(
            Query.SCHEMA, QueryHandler.with(QueryInputGltfSchema.class, this::getSchemaResponse));
    this.schemaCache = new Metadata3dSchemaCacheImpl();
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getSchemaResponse(
      QueryInputGltfSchema queryInput, ApiRequestContext requestContext) {

    return getResponse(queryInput, requestContext, providers, i18n);
  }

  private Response getResponse(
      QueryInputGltfSchema queryInput,
      ApiRequestContext requestContext,
      FeaturesCoreProviders providers,
      I18n i18n) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    String collectionId = queryInput.getCollectionId();
    checkCollectionId(apiData, collectionId);
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

    SchemaFormat3dMetadata outputFormat =
        api.getOutputFormat(
                SchemaFormat3dMetadata.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/gltf/schema",
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

    List<Link> links =
        new DefaultLinksGenerator()
            .generateLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                alternateMediaTypes,
                i18n,
                requestContext.getLanguage());

    FeatureSchema featureSchema =
        providers
            .getFeatureSchema(apiData, collectionData)
            .orElse(
                new ImmutableFeatureSchema.Builder()
                    .name(collectionId)
                    .type(SchemaBase.Type.OBJECT)
                    .build());

    GltfSchema schema =
        schemaCache.getSchema(
            featureSchema,
            apiData,
            collectionId,
            apiData
                .getExtension(GltfConfiguration.class, collectionId)
                .map(GltfConfiguration::getProperties)
                .orElseThrow(),
            entityRegistry.getEntitiesForType(Codelist.class),
            GltfHelper.withSurfaceTypes(featureSchema));

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class, collectionId)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(schema, GltfSchema.FUNNEL, outputFormat.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s.schema.%s", collectionId, outputFormat.getMediaType().fileExtension())))
        .entity(outputFormat.getEntity(schema))
        .build();
  }

  // TODO centralize
  static void checkCollectionId(OgcApiDataV2 apiData, String collectionId) {
    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          String.format("The collection '%s' does not exist in this API.", collectionId));
    }
  }
}
