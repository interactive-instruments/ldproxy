/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.collections.schema.domain.QueriesHandlerSchema.Query;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
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
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.web.domain.ETag;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.immutables.value.Value;

@AutoMultiBind
public interface QueriesHandlerSchema extends QueriesHandler<Query> {

  enum Query implements QueryIdentifier {
    SCHEMA
  }

  @Value.Immutable
  interface QueryInputSchema extends QueryInput {
    String getCollectionId();

    boolean getIncludeLinkHeader();

    Optional<String> getProfile();

    String getType();
  }

  List<String> getSupportedTypes();

  @Override
  default boolean canHandle(Query queryIdentifier, QueryInput queryInput) {
    return queryInput instanceof QueryInputSchema
        && getSupportedTypes().contains(((QueryInputSchema) queryInput).getType());
  }

  default Response getResponse(
      QueryInputSchema queryInput,
      ApiRequestContext requestContext,
      FeaturesCoreProviders providers,
      I18n i18n) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    String collectionId = queryInput.getCollectionId();
    checkCollectionId(apiData, collectionId);
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

    SchemaFormatExtension outputFormat =
        api.getOutputFormat(
                SchemaFormatExtension.class,
                requestContext.getMediaType(),
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

    Optional<String> schemaUri =
        links.stream()
            .filter(link -> Objects.equals(link.getRel(), "self"))
            .map(Link::getHref)
            .findAny();

    FeatureSchema featureSchema =
        providers
            .getFeatureSchema(apiData, collectionData)
            .orElse(
                new ImmutableFeatureSchema.Builder()
                    .name(collectionId)
                    .type(SchemaBase.Type.OBJECT)
                    .build());

    JsonSchemaDocument schema =
        getJsonSchema(
            featureSchema,
            apiData,
            collectionData,
            schemaUri,
            getVersion(queryInput.getProfile()),
            queryInput.getType());

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class, collectionId)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(schema, JsonSchema.FUNNEL, outputFormat.getMediaType().label())
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
        .entity(outputFormat.getEntity(schema, collectionId, api, requestContext))
        .build();
  }

  JsonSchemaDocument getJsonSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version,
      String type);

  static void checkCollectionId(OgcApiDataV2 apiData, String collectionId) {
    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          String.format("The collection '%s' does not exist in this API.", collectionId));
    }
  }

  static VERSION getVersion(Optional<String> profile) {
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
