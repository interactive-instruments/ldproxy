/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;


@Value.Immutable
public abstract class AbstractOgcApiRequestContext implements OgcApiRequestContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOgcApiRequestContext.class);

    abstract URI getRequestUri();

    abstract Optional<URI> getExternalUri();

    @Override
    public abstract OgcApiMediaType getMediaType();

    @Override
    public abstract List<OgcApiMediaType> getAlternateMediaTypes();

    @Override
    public abstract Optional<Locale> getLanguage();

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
                    .ensureTrailingSlash()
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
                                     LOGGER.error("Duplicate parameter found, the following value is ignored: " + value2);
                                     return value1;
                                 }));
    }
}
