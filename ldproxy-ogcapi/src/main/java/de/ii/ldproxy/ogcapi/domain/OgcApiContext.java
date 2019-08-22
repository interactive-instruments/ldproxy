/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Value.Immutable
public interface OgcApiContext {
    enum HttpMethods {GET, POST, PUT, DELETE, PATCH}

    String getApiEntrypoint();

    Optional<String> getSubPathPattern();

    @Value.Default
    default List<HttpMethods> getMethods() {
        return ImmutableList.of();
    }

    @Value.Derived
    @Value.Auxiliary
    default Optional<Pattern> getSubPathPatternCompiled() {
        return getSubPathPattern().map(Pattern::compile);
    }

    @Value.Derived
    @Value.Auxiliary
    default List<String> getMethodStrings() {
        return getMethods().stream()
                           .map(Enum::name)
                           .collect(ImmutableList.toImmutableList());
    }

    default boolean matches(String firstPathSegment, String subPath, String method) {
        boolean matchesPath = firstPathSegment.matches(getApiEntrypoint());
        boolean matchesSubPath = !getSubPathPatternCompiled().isPresent() || getSubPathPatternCompiled().get()
                                                                                                        .matcher(subPath)
                                                                                                        .matches();
        boolean matchesMethod = getMethods().isEmpty() || getMethodStrings().contains(method);

        return matchesPath && matchesMethod && matchesSubPath;
    }
}
