/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Value.Immutable
public interface OgcApiContext {
    enum HttpMethods {GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS}

    String getApiEntrypoint();

    Optional<String> getSubPathPattern();

    Map<String, List<HttpMethods>> getSubPathsAndMethods();

    @Value.Default
    default List<HttpMethods> getMethods() {
        return ImmutableList.of();
    }

    @Value.Default
    default List<HttpMethods> getMethods(Optional<String> subPathPattern) {
        return ImmutableList.of();
    }

    @Value.Derived
    @Value.Auxiliary
    default Optional<Pattern> getSubPathPatternCompiled() {
        return getSubPathPattern().map(Pattern::compile);
    }

    @Value.Derived
    @Value.Auxiliary
    default List<String> getMethodStrings(boolean withOptions) {
        return getMethods().stream()
                           .map(Enum::name)
                           .filter(method -> withOptions || !method.equalsIgnoreCase("OPTIONS"))
                           .collect(ImmutableList.toImmutableList());
    }

    @Value.Derived
    @Value.Auxiliary
    default Map<Optional<Pattern>, List<String>> getSubPathsAndMethodsProcessed(boolean withOptions) {
        if (getSubPathsAndMethods().isEmpty()) {
            return new ImmutableMap.Builder()
                    .put(getSubPathPatternCompiled(), getMethodStrings(withOptions))
                    .build();
        }

        return getSubPathsAndMethods().entrySet().stream()
                .map(pathAndMethods -> new AbstractMap.SimpleImmutableEntry<Optional<Pattern>,List<String>>(
                        Optional.ofNullable(Pattern.compile(pathAndMethods.getKey())),
                        pathAndMethods.getValue()
                                .stream()
                                .map(Enum::name)
                                .filter(method -> withOptions || !method.equalsIgnoreCase("OPTIONS"))
                                .collect(ImmutableList.toImmutableList())))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default boolean matches(String firstPathSegment, String subPath, String method) {
        boolean matchesPath = firstPathSegment.matches(getApiEntrypoint());
        boolean matchesSubPath = !getSubPathPatternCompiled().isPresent() || getSubPathPatternCompiled().get()
                                                                                                        .matcher(subPath)
                                                                                                        .matches();
        boolean matchesMethod = getSubPathsAndMethodsProcessed(method!=null).entrySet().stream()
                    .anyMatch(entry -> entry.getKey().orElse(Pattern.compile(".*")).matcher(subPath).matches() &&
                            ((method==null && entry.getValue().size()>0) || (method!=null && entry.getValue().contains(method))));

        return matchesPath && matchesMethod && matchesSubPath;
    }
}
