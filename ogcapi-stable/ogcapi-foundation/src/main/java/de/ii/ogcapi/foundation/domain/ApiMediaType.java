/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import static javax.ws.rs.core.MediaType.MEDIA_TYPE_WILDCARD;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.MediaType;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public interface ApiMediaType {

  Logger LOGGER = LoggerFactory.getLogger(ApiMediaType.class);

  enum CompatibilityLevel {
    PARAMETERS,
    SUBTYPES,
    TYPES
  }

  MediaType type();

  @Value.Default
  default String label() {
    return type().getSubtype().toUpperCase(Locale.ROOT);
  }

  @Value.Default
  default String parameter() {
    return type().getSubtype().contains("+")
        ? type().getSubtype().substring(type().getSubtype().lastIndexOf("+") + 1)
        : type().getSubtype();
  }

  @Value.Default
  default String fileExtension() {
    return parameter();
  }

  @Value.Default
  default int qs() {
    return 1000;
  }

  default boolean matches(MediaType mediaType) {
    return Objects.nonNull(
        negotiateMediaType(ImmutableList.of(type()), ImmutableList.of(mediaType)));
  }

  static boolean isCompatible(MediaType accepted, MediaType provided, CompatibilityLevel level) {
    if (provided == null) {
      return false;
    }

    boolean result = false;

    if (level == CompatibilityLevel.TYPES
        || level == CompatibilityLevel.SUBTYPES
        || level == CompatibilityLevel.PARAMETERS) {
      result =
          accepted.getType().equals(MEDIA_TYPE_WILDCARD)
              || provided.getType().equals(MEDIA_TYPE_WILDCARD)
              || accepted.getType().equalsIgnoreCase(provided.getType());
    }

    if (result
        && (level == CompatibilityLevel.SUBTYPES || level == CompatibilityLevel.PARAMETERS)) {
      result =
          accepted.getSubtype().equals(MEDIA_TYPE_WILDCARD)
              || provided.getSubtype().equals(MEDIA_TYPE_WILDCARD)
              || accepted.getSubtype().equalsIgnoreCase(provided.getSubtype())
              || provided.getSubtype().endsWith("+" + accepted.getSubtype());
    }

    if (result && level == CompatibilityLevel.PARAMETERS) {
      Map<String, String> acceptedParameters = accepted.getParameters();
      Map<String, String> providedParameters = provided.getParameters();
      result =
          acceptedParameters.entrySet().stream()
                  .allMatch(
                      entry ->
                          providedParameters.containsKey(entry.getKey())
                              && providedParameters.get(entry.getKey()).equals(entry.getValue()))
              && providedParameters.entrySet().stream()
                  .allMatch(
                      entry ->
                          acceptedParameters.containsKey(entry.getKey())
                              && acceptedParameters.get(entry.getKey()).equals(entry.getValue()));
    }

    return result;
  }

  static MediaType negotiateMediaType(
      List<MediaType> acceptableMediaTypes, List<MediaType> providedMediaTypes) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("acceptable: {}", acceptableMediaTypes);
      LOGGER.debug("provided: {}", providedMediaTypes);
    }
    for (CompatibilityLevel level : CompatibilityLevel.values()) {
      for (MediaType acceptableMediaType : acceptableMediaTypes) {
        for (MediaType providedMediaType : providedMediaTypes) {
          if (ApiMediaType.isCompatible(acceptableMediaType, providedMediaType, level)) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("selected: {}", providedMediaType);
            }
            return providedMediaType;
          }
        }
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("selected: null");
    }
    return null;
  }
}
