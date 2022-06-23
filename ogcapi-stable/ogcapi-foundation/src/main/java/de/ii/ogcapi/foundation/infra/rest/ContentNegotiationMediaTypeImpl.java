/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class ContentNegotiationMediaTypeImpl implements ContentNegotiationMediaType {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ContentNegotiationMediaTypeImpl.class);
  private static final String CONTENT_TYPE_PARAMETER = "f";
  private static final String ACCEPT_HEADER = "Accept";

  private static final List<String> USER_AGENT_BOTS =
      ImmutableList.of(
          "googlebot",
          "bingbot",
          "duckduckbot",
          "yandexbot",
          "baiduspider",
          "slurp",
          "exabot",
          "facebot",
          "ia_archiver");

  @Inject
  ContentNegotiationMediaTypeImpl() {}

  @Override
  public Optional<ApiMediaType> negotiateMediaType(
      ContainerRequestContext requestContext, Set<ApiMediaType> supportedMediaTypes) {

    evaluateFormatParameter(
        supportedMediaTypes,
        requestContext.getUriInfo().getQueryParameters(),
        requestContext.getHeaders());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("accept {}", requestContext.getHeaderString(ACCEPT_HEADER));
    }

    Optional<ApiMediaType> ogcApiMediaType =
        negotiateMediaType(supportedMediaTypes, requestContext.getRequest());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("content-type {}", ogcApiMediaType);
    }

    return ogcApiMediaType;
  }

  @Override
  public Optional<ApiMediaType> negotiateMediaType(
      Request request,
      HttpHeaders httpHeaders,
      UriInfo uriInfo,
      Set<ApiMediaType> supportedMediaTypes) {

    evaluateFormatParameter(
        supportedMediaTypes, uriInfo.getQueryParameters(), httpHeaders.getRequestHeaders());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("accept {}", httpHeaders.getHeaderString(ACCEPT_HEADER));
    }

    Optional<ApiMediaType> ogcApiMediaType = negotiateMediaType(supportedMediaTypes, request);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("content-type {}", ogcApiMediaType);
    }

    return ogcApiMediaType;
  }

  private void evaluateFormatParameter(
      Set<ApiMediaType> supportedMediaTypes,
      MultivaluedMap<String, String> queryParameters,
      MultivaluedMap<String, String> headers) {

    String format = null;
    if (queryParameters.containsKey(CONTENT_TYPE_PARAMETER)) {
      format = queryParameters.getFirst(CONTENT_TYPE_PARAMETER);
    } else {
      // use crawler user agent headers to trigger an implicit f=html
      String userAgent = headers.getFirst("user-agent");
      if (Objects.nonNull(userAgent)) {
        String finalUserAgent = userAgent.toLowerCase(Locale.ROOT);
        if (USER_AGENT_BOTS.stream().anyMatch(finalUserAgent::contains)) {
          format = "html";
        }
      }
    }

    if (Objects.nonNull(format)) {
      String finalFormat = format;
      Optional<ApiMediaType> ogcApiMediaType =
          supportedMediaTypes.stream()
              .filter(mediaType -> Objects.equals(mediaType.parameter(), finalFormat))
              .findFirst();
      ogcApiMediaType.ifPresent(
          apiMediaType -> headers.putSingle(ACCEPT_HEADER, apiMediaType.type().toString()));
    }
  }

  private Optional<ApiMediaType> negotiateMediaType(
      Set<ApiMediaType> supportedMediaTypes, Request request) {
    MediaType[] supportedMediaTypesArray =
        supportedMediaTypes.stream().flatMap(this::toTypes).distinct().toArray(MediaType[]::new);

    Variant variant = null;
    try {
      if (supportedMediaTypesArray.length > 0) {
        variant = request.selectVariant(Variant.mediaTypes(supportedMediaTypesArray).build());
      }
    } catch (Exception ex) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Could not parse request headers during content negotiation. Selecting any media type. Reason: {}",
            ex.getMessage());
      }
      return supportedMediaTypes.stream().findAny();
    }

    return Optional.ofNullable(variant)
        .map(Variant::getMediaType)
        .flatMap(mediaType -> findMatchingOgcApiMediaType(mediaType, supportedMediaTypes));
  }

  private Optional<ApiMediaType> findMatchingOgcApiMediaType(
      MediaType mediaType, Set<ApiMediaType> supportedMediaTypes) {
    return supportedMediaTypes.stream().filter(type -> type.matches(mediaType)).findFirst();
  }

  private Stream<MediaType> toTypes(ApiMediaType apiMediaType) {
    return Stream.of(apiMediaType.type())
        .map(
            mediaType ->
                apiMediaType.qs() < 1000
                    ? new QualitySourceMediaType(
                        mediaType.getType(),
                        mediaType.getSubtype(),
                        apiMediaType.qs(),
                        mediaType.getParameters())
                    : mediaType);
  }
}
