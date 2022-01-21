/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import java.net.URI;
import java.util.*;


@Value.Immutable
public abstract class AbstractRequestContext implements ApiRequestContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRequestContext.class);

    abstract URI getRequestUri();

    abstract Optional<URI> getExternalUri();

    @Override
    public abstract ApiMediaType getMediaType();

    @Override
    public abstract List<ApiMediaType> getAlternateMediaTypes();

    @Override
    public abstract Optional<Locale> getLanguage();

    @Override
    public abstract Optional<Request> getRequest();

    @Value.Derived
    @Override
    public URICustomizer getUriCustomizer() {
        URICustomizer uriCustomizer = new URICustomizer(getRequestUri());

        if (getExternalUri().isPresent()) {
            uriCustomizer.setScheme(getExternalUri().get()
                                                    .getScheme());
            uriCustomizer.replaceInPath("/rest/services", getExternalUri().get()
                                                                          .getPath());
        }

        return uriCustomizer;
    }

    @Value.Derived
    @Override
    public String getStaticUrlPrefix() {
        String staticUrlPrefix = "";

        if (getExternalUri().isPresent()) {
            staticUrlPrefix = new URICustomizer(getRequestUri())
                    .cutPathAfterSegments("rest", "services")
                    .replaceInPath("/rest/services", getExternalUri().get()
                                                                     .getPath())
                    .ensureLastPathSegment("___static___")
                    .ensureNoTrailingSlash()
                    .getPath();
        }

        return staticUrlPrefix;
    }

    @Value.Derived
    @Override
    public Map<String, String> getParameters() {
        return getUriCustomizer().getQueryParams()
                                 .stream()
                                 .map(nameValuePair -> new AbstractMap.SimpleImmutableEntry<>(nameValuePair.getName(), nameValuePair.getValue()))
                                 .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> {
                                     // TODO for now ignore multiple parameters with the same name
                                     LOGGER.warn("Duplicate parameter found, the following value is ignored: " + value2);
                                     return value1;
                                 }));
    }
}
