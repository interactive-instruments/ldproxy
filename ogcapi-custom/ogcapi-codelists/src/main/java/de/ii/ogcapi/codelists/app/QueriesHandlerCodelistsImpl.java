/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.codelists.domain.CodelistEntry;
import de.ii.ogcapi.codelists.domain.CodelistFormatExtension;
import de.ii.ogcapi.codelists.domain.Codelists;
import de.ii.ogcapi.codelists.domain.CodelistsFormatExtension;
import de.ii.ogcapi.codelists.domain.ImmutableCodelistEntry;
import de.ii.ogcapi.codelists.domain.ImmutableCodelists;
import de.ii.ogcapi.codelists.domain.QueriesHandlerCodelists;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.base.domain.ETag;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueStore;
import de.ii.xtraplatform.values.domain.Values;
import de.ii.xtraplatform.web.domain.LastModified;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.hc.core5.net.PercentCodec;

@Singleton
@AutoBind
public class QueriesHandlerCodelistsImpl extends AbstractVolatileComposed
    implements QueriesHandlerCodelists {

  private final I18n i18n;
  private final ExtensionRegistry extensionRegistry;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final Values<Codelist> codelistStore;
  private final FeaturesCoreProviders providers;

  @Inject
  public QueriesHandlerCodelistsImpl(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      I18n i18n,
      ValueStore valueStore,
      VolatileRegistry volatileRegistry) {
    super(QueriesHandlerCodelists.class.getSimpleName(), volatileRegistry, true);
    this.extensionRegistry = extensionRegistry;
    this.i18n = i18n;
    this.codelistStore = valueStore.forType(Codelist.class);
    this.providers = providers;
    this.queryHandlers =
        ImmutableMap.of(
            Query.CODELISTS,
                QueryHandler.with(QueryInputCodelists.class, this::getCodelistsResponse),
            Query.CODELIST, QueryHandler.with(QueryInputCodelist.class, this::getCodelistResponse));

    onVolatileStart();

    addSubcomponent(codelistStore);

    onVolatileStarted();
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getCodelistsResponse(
      QueryInputCodelists queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    CodelistsLinkGenerator codelistsLinkGenerator = new CodelistsLinkGenerator();

    CodelistsFormatExtension format =
        extensionRegistry.getExtensionsForType(CodelistsFormatExtension.class).stream()
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .map(CodelistsFormatExtension.class::cast)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type {0} cannot be generated.",
                            requestContext.getMediaType().type())));

    Set<String> codelistIds =
        providers
            .getFeatureProvider(apiData)
            .map(
                prov ->
                    prov.info().getSchemas().stream()
                        .map(FeatureSchema::getAllNestedProperties)
                        .flatMap(List::stream)
                        .flatMap(
                            prop ->
                                prop.getConstraints().stream()
                                    .map(SchemaConstraints::getCodelist)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get))
                        .collect(Collectors.toSet()))
            .orElse(ImmutableSet.of());

    List<CodelistEntry> codelistEntries =
        codelistStore.identifiers().stream()
            .filter(identifier -> codelistIds.contains(identifier.asPath()))
            .map(
                identifier -> {
                  Codelist codelist = codelistStore.get(identifier);
                  String encodedId =
                      PercentCodec.encode(identifier.asPath(), StandardCharsets.UTF_8);

                  return ImmutableCodelistEntry.builder()
                      .id(encodedId)
                      .title(codelist.getLabel())
                      .lastModified(LastModified.from(codelistStore.lastModified(identifier)))
                      .addLinks(
                          codelistsLinkGenerator.generateCodelistLink(
                              requestContext.getUriCustomizer(), encodedId, codelist.getLabel()))
                      .build();
                })
            .collect(Collectors.toList());

    Optional<Date> lastModified =
        codelistEntries.stream()
            .map(CodelistEntry::getLastModified)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .max(Date::compareTo);

    Codelists codelists =
        ImmutableCodelists.builder()
            .title(i18n.get("codelistsTitle", requestContext.getLanguage()))
            .description(i18n.get("codelistsDescription", requestContext.getLanguage()))
            .codelistEntries(codelistEntries)
            .lastModified(lastModified)
            .links(getLinks(requestContext, i18n))
            .build();

    EntityTag etag =
        !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(codelists, Codelists.FUNNEL, format.getMediaType().label())
            : null;
    Response.ResponseBuilder response =
        evaluatePreconditions(requestContext, lastModified.orElse(null), etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? codelists.getLinks() : null,
            HeaderCaching.of(lastModified.orElse(null), etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("codelists.%s", format.getMediaType().fileExtension())))
        .entity(format.getCodelistsEntity(codelists, apiData, requestContext))
        .build();
  }

  private Response getCodelistResponse(
      QueryInputCodelist queryInput, ApiRequestContext requestContext) {
    OgcApi api = requestContext.getApi();
    OgcApiDataV2 apiData = api.getData();
    String encodedId = queryInput.getCodelistId();
    Identifier identifier =
        Identifier.from(Path.of(PercentCodec.decode(encodedId, StandardCharsets.UTF_8)));
    String decodedId = identifier.asPath();

    final CodelistFormatExtension format =
        extensionRegistry.getExtensionsForType(CodelistFormatExtension.class).stream()
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .map(CodelistFormatExtension.class::cast)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type {0} cannot be generated.",
                            requestContext.getMediaType().type())));

    providers
        .getFeatureProvider(apiData)
        .filter(
            prov ->
                prov.info().getSchemas().stream()
                    .map(FeatureSchema::getAllNestedProperties)
                    .flatMap(List::stream)
                    .flatMap(
                        prop ->
                            prop.getConstraints().stream()
                                .map(SchemaConstraints::getCodelist)
                                .filter(Optional::isPresent)
                                .map(Optional::get))
                    .anyMatch(decodedId::equals))
        .orElseThrow(
            () ->
                new NotFoundException(
                    MessageFormat.format("The codelist ''{0}'' does not exist.", encodedId)));

    Codelist codelist = codelistStore.get(identifier);
    if (codelist == null) {
      throw new NotFoundException(
          MessageFormat.format("The codelist ''{0}'' does not exist.", encodedId));
    }

    codelist = new ImmutableCodelist.Builder().from(codelist).sourceType(Optional.empty()).build();

    Date lastModified = LastModified.from(codelistStore.lastModified(identifier));

    String hash = codelist.getStableHash();
    EntityTag eTag = hash != null ? EntityTag.valueOf(hash) : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, eTag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            null,
            HeaderCaching.of(lastModified, eTag, queryInput),
            null,
            HeaderContentDisposition.of(encodedId))
        .entity(format.getCodelist(codelist, encodedId, apiData, requestContext))
        .type(format.getMediaType().type())
        .build();
  }
}
