/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.immutables.value.Value;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
//@JsonDeserialize(as = ImmutableOgcApiRequestContextImpl.class)
public abstract class AbstractOgcApiRequestContext implements OgcApiRequestContext {

    abstract URI getRequestUri();

    abstract Optional<URI> getExternalUri();

    @Override
    public abstract OgcApiMediaType getMediaType();

    @Override
    public abstract List<OgcApiMediaType> getAlternativeMediaTypes();

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
    public URICustomizer getUriCustomizerPlain() {
        return new URICustomizer(getRequestUri());
    }

    @Value.Derived
    @Override
    public String getStaticUrlPrefix() {
        String staticUrlPrefix = "";

        if (getExternalUri().isPresent()) {
            String path = getExternalUri().get()
                            .getPath();
            if (path != null) {
                staticUrlPrefix = new URICustomizer(getRequestUri())
                        .cutPathAfterSegments("rest", "services")
                        .replaceInPath("/rest/services", path)
                        .ensureTrailingSlash()
                        .ensureLastPathSegment("___static___")
                        .getPath();
            }
        }

        return staticUrlPrefix;
    }
}
