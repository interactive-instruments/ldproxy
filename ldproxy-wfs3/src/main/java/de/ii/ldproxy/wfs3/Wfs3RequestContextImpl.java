/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableWfs3RequestContextImpl.class)
public abstract class Wfs3RequestContextImpl implements Wfs3RequestContext {

    abstract URI getRequestUri();

    abstract Optional<URI> getExternalUri();

    @Override
    public abstract Wfs3MediaType getMediaType();

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
                    .ensureTrailingSlash()
                    .ensureLastPathSegment("___static___")
                    .getPath();
        }

        return staticUrlPrefix;
    }
}
