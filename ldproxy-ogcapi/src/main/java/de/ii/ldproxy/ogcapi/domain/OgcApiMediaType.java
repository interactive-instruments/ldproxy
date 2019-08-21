/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
public interface OgcApiMediaType {

    MediaType main();

    @Value.Default
    default MediaType metadata() {
        return main();
    }

    @Value.Default
    default String label() {
        return main().getSubtype().toUpperCase();
    }

    @Value.Default
    default String metadataLabel() {
        return metadata().getSubtype().toUpperCase();
    }

    @Value.Default
    default String parameter() {
        return main().getSubtype().contains("+") ? main().getSubtype().substring(main().getSubtype().lastIndexOf("+")+1)  : main().getSubtype();
    }

    @Value.Default
    default int qs() {
        return 1000;
    }

    default boolean matches(MediaType mediaType) {
        return main().isCompatible(mediaType) || metadata().isCompatible(mediaType) /*|| alternative().map(mediaType1 -> mediaType1.isCompatible(mediaType)).orElse(false)*/;
    }

}
