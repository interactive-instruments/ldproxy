/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.infra.rest.ContentNegotiation;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.MEDIA_TYPE_WILDCARD;


@Value.Immutable
public interface ApiMediaType {

    Logger LOGGER = LoggerFactory.getLogger(ApiMediaType.class);
    enum COMPATIBILITY_LEVEL { PARAMETERS, SUBTYPES, TYPES }

    MediaType type();

    @Value.Default
    default String label() {
        return type().getSubtype().toUpperCase();
    }

    @Value.Default
    default String parameter() {
        return type().getSubtype().contains("+") ? type().getSubtype().substring(type().getSubtype().lastIndexOf("+")+1)  : type().getSubtype();
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
        return Objects.nonNull(negotiateMediaType(ImmutableList.of(type()), ImmutableList.of(mediaType)));
    }

    static boolean isCompatible(MediaType accepted, MediaType provided, COMPATIBILITY_LEVEL level) {
        boolean result = false;

        LOGGER.debug("isCompatible-1: '{}', '{}', '{}': {}", accepted, provided, level, result);

        if (provided==null)
            return result;

        if (level==COMPATIBILITY_LEVEL.TYPES || level==COMPATIBILITY_LEVEL.SUBTYPES || level==COMPATIBILITY_LEVEL.PARAMETERS) {
            result = accepted.getType().equals(MEDIA_TYPE_WILDCARD)
                || provided.getType().equals(MEDIA_TYPE_WILDCARD)
                || (accepted.getType().equalsIgnoreCase(provided.getType()));
        }

        LOGGER.debug("isCompatible-2: '{}', '{}', '{}': {}", accepted, provided, level, result);

        if (result && (level==COMPATIBILITY_LEVEL.SUBTYPES || level==COMPATIBILITY_LEVEL.PARAMETERS)) {
            result = accepted.getSubtype().equals(MEDIA_TYPE_WILDCARD)
                || provided.getSubtype().equals(MEDIA_TYPE_WILDCARD)
                || accepted.getSubtype().equalsIgnoreCase(provided.getSubtype())
                || provided.getSubtype().endsWith("+" + accepted.getSubtype());
        }

        LOGGER.debug("isCompatible-3: '{}', '{}', '{}': {}", accepted, provided, level, result);

        if (result && level==COMPATIBILITY_LEVEL.PARAMETERS) {
            Map<String, String> acceptedParameters = accepted.getParameters();
            Map<String, String> providedParameters = provided.getParameters();
            result = acceptedParameters.entrySet()
                .stream()
                .allMatch(entry -> providedParameters.containsKey(entry.getKey())
                    && providedParameters.get(entry.getKey()).equals(entry.getValue()))
                &&
                providedParameters.entrySet()
                    .stream()
                    .allMatch(entry -> acceptedParameters.containsKey(entry.getKey())
                        && acceptedParameters.get(entry.getKey()).equals(entry.getValue()));
        }

        LOGGER.debug("isCompatible-4: '{}', '{}', '{}': {}", accepted, provided, level, result);

        return result;
    }

    static MediaType negotiateMediaType(List<MediaType> acceptableMediaTypes, List<MediaType> providedMediaTypes) {
        LOGGER.debug("acceptable: {}", acceptableMediaTypes);
        LOGGER.debug("provided: {}", providedMediaTypes);
        for (ApiMediaType.COMPATIBILITY_LEVEL level : ApiMediaType.COMPATIBILITY_LEVEL.values()) {
            for (MediaType acceptableMediaType : acceptableMediaTypes) {
                for (MediaType providedMediaType : providedMediaTypes) {
                    if (ApiMediaType.isCompatible(acceptableMediaType, providedMediaType, level)) {
                        LOGGER.debug("selected: {}", providedMediaType);
                        return providedMediaType;
                    }
                }
            }
        }
        LOGGER.debug("selected: null");
        return null;
    }
}
