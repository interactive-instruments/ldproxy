/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Endpoint implements EndpointExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint.class);

  protected final ExtensionRegistry extensionRegistry;
  private final Map<Integer, ApiEndpointDefinition> apiDefinitions;
  protected List<? extends FormatExtension> formats;

  public Endpoint(ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
    this.apiDefinitions = new ConcurrentHashMap<>();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    try {
      // compile and cache the API definition
      ApiEndpointDefinition definition = getDefinition(api.getData());

      if (getFormats().isEmpty()
          && definition.getResources().values().stream()
              .map(r -> r.getOperations().keySet())
              .flatMap(Collection::stream)
              .anyMatch("get"::equalsIgnoreCase)) {

        builder.addStrictErrors(
            MessageFormat.format(
                "The Endpoint class ''{0}'' does not support any output format.",
                this.getClass().getSimpleName()));
      }

    } catch (Exception exception) {
      String message = exception.getMessage();
      if (Objects.isNull(message)) {
        message =
            exception.getClass().getSimpleName() + " at " + exception.getStackTrace()[0].toString();
      }
      builder.addErrors(message);
    }

    return builder.build();
  }

  @Override
  public final ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
    if (!isEnabledForApi(apiData)) {
      return EndpointExtension.super.getDefinition(apiData);
    }

    return apiDefinitions.computeIfAbsent(
        apiData.hashCode(),
        ignore -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Generating API definition for {}", this.getClass().getSimpleName());
          }

          ApiEndpointDefinition apiEndpointDefinition = computeDefinition(apiData);

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Finished generating API definition for {}", this.getClass().getSimpleName());
          }

          return apiEndpointDefinition;
        });
  }

  protected abstract ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData);

  /**
   * @return the list of output format candidates for this endpoint
   */
  public abstract List<? extends FormatExtension> getFormats();

  public Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters) {
    return toFlatMap(queryParameters, false);
  }

  protected Map<String, String> toFlatMap(
      MultivaluedMap<String, String> queryParameters, boolean keysToLowerCase) {
    return queryParameters.entrySet().stream()
        .map(
            entry -> {
              String key =
                  keysToLowerCase ? entry.getKey().toLowerCase(Locale.ROOT) : entry.getKey();
              return new AbstractMap.SimpleImmutableEntry<>(
                  key,
                  entry.getValue().isEmpty()
                      ? ""
                      : Objects.requireNonNullElse(entry.getValue().get(0), ""));
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<MediaType, ApiMediaTypeContent> getContent(OgcApiDataV2 apiData, String path) {
    return getFormats().stream()
        .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
        .map(f -> f.getContent(apiData, path))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  protected Map<MediaType, ApiMediaTypeContent> getContent(
      OgcApiDataV2 apiData, String path, HttpMethods method) {
    return getFormats().stream()
        .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
        .map(f -> f.getContent(apiData, path, method))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  public QueryInput getGenericQueryInput(OgcApiDataV2 apiData) {
    ImmutableQueryInputGeneric.Builder queryInputBuilder =
        new ImmutableQueryInputGeneric.Builder()
            .includeLinkHeader(
                apiData
                    .getExtension(FoundationConfiguration.class)
                    .map(FoundationConfiguration::getIncludeLinkHeader)
                    .orElse(false));

    apiData
        .getExtension(getBuildingBlockConfigurationType())
        .filter(moduleConfig -> moduleConfig instanceof CachingConfiguration)
        .map(moduleConfig -> (CachingConfiguration) moduleConfig)
        .ifPresent(
            moduleConfig -> {
              Optional<Caching> caching = Optional.ofNullable(moduleConfig.getCaching());
              Optional<Caching> defaultCaching = apiData.getDefaultCaching();
              caching
                  .map(Caching::getLastModified)
                  .or(() -> defaultCaching.map(Caching::getLastModified))
                  .ifPresent(queryInputBuilder::lastModified);
              caching
                  .map(Caching::getExpires)
                  .or(() -> defaultCaching.map(Caching::getExpires))
                  .ifPresent(queryInputBuilder::expires);
              caching
                  .map(Caching::getCacheControl)
                  .or(() -> defaultCaching.map(Caching::getCacheControl))
                  .ifPresent(queryInputBuilder::cacheControl);
            });

    return queryInputBuilder.build();
  }

  protected boolean strictHandling(Enumeration<String> prefer) {
    var hints =
        new Object() {
          boolean strict;
          boolean lenient;
        };
    Collections.list(prefer)
        .forEach(
            header -> {
              hints.strict = hints.strict || header.contains("handling=strict");
              hints.lenient = hints.lenient || header.contains("handling=lenient");
            });
    if (hints.strict && hints.lenient) {
      throw new IllegalArgumentException(
          "The request contains preferences for both strict and lenient processing. Both preferences are incompatible with each other.");
    }
    return hints.strict;
  }

  /**
   * create MediaType from text string; if the input string has problems, the value defaults to
   * wildcards
   *
   * @param mediaTypeString the media type as a string
   * @return the processed media type
   */
  public static MediaType mediaTypeFromString(String mediaTypeString) {
    String[] typeAndSubtype = mediaTypeString.split("/", 2);
    if (typeAndSubtype[0].matches(
        "application|audio|font|example|image|message|model|multipart|text|video")) {
      if (typeAndSubtype.length == 1) {
        // no subtype
        return new MediaType(typeAndSubtype[0], "*");
      } else {
        // we have a subtype - and maybe parameters
        String[] subtypeAndParameters = typeAndSubtype[1].split(";");
        int count = subtypeAndParameters.length;
        if (count == 1) {
          // no parameters
          return new MediaType(typeAndSubtype[0], subtypeAndParameters[0]);
        } else {
          // we have at least one parameter
          Map<String, String> params =
              IntStream.rangeClosed(1, count - 1)
                  .mapToObj(i -> subtypeAndParameters[i].split("=", 2))
                  .filter(nameValuePair -> nameValuePair.length == 2)
                  .map(
                      nameValuePair ->
                          new AbstractMap.SimpleImmutableEntry<>(
                              nameValuePair[0].trim(), nameValuePair[1].trim()))
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
          return new MediaType(typeAndSubtype[0], subtypeAndParameters[0], params);
        }
      }
    } else {
      // not a valid type, fall back to wildcard
      return MediaType.WILDCARD_TYPE;
    }
  }

  protected Optional<String> getOperationId(String name, String... prefixes) {
    return Optional.of(
        prefixes.length > 0 ? String.format("%s.%s", String.join(".", prefixes), name) : name);
  }
}
