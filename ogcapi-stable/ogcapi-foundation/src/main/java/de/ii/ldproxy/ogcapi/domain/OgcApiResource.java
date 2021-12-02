/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.infra.rest.ExceptionMapper;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A resource is any path/URI supported by the API. Through path parameters any instance may represent
 * multiple resources of the API.
 */
public interface OgcApiResource {

    Logger LOGGER = LoggerFactory.getLogger(OgcApiResource.class);

    String getPath();

    List<OgcApiPathParameter> getPathParameters();
    Map<String, ApiOperation> getOperations();

    default boolean isSubCollectionWithExplicitId(OgcApiDataV2 apiData) {
        return getPathParameters().stream().anyMatch(param -> param.getName().equals("collectionId") && param.getExplodeInOpenApi(apiData));
    }

    default Optional<String> getCollectionId(OgcApiDataV2 apiData) {
        return isSubCollectionWithExplicitId(apiData)? Optional.ofNullable(Splitter.on("/").limit(3).omitEmptyStrings().splitToList(getPath()).get(1)) :Optional.empty();
    }

    @Value.Lazy
    default String getPathPattern() {
        String path = getPath();
        for (OgcApiPathParameter param : getPathParameters()) {
            path = path.replace("{"+param.getName()+"}", param.getPattern());
        }
        if (path.contains("{")) {
            LOGGER.error("Could not resolve a path parameter to a pattern. This should not occur and will lead to errors (requires a fix in the code). Using '.*' as a fallback. Resource path: {}", path);
            path = path.replaceAll("\\{\\w+\\}",".*");
        }
        return "^(?:"+path+")/?$";
    }

    @Value.Lazy
    default Pattern getPathPatternCompiled() {
        return Pattern.compile(getPathPattern());
    }
}
