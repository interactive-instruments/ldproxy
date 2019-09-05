/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Value.Immutable
public interface OgcApiMediaType {

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
    default int qs() {
        return 1000;
    }

    default boolean matches(MediaType mediaType) {
        return type().isCompatible(mediaType);
    }

}
