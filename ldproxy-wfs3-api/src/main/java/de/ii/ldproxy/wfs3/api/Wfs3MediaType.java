/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class Wfs3MediaType {

    public abstract MediaType main();

    public abstract Optional<MediaType> alternative();

    @Value.Default
    public MediaType metadata() {
        return main();
    }

    @Value.Default
    public String label() {
        return main().getSubtype().toUpperCase();
    }

    @Value.Default
    public String metadataLabel() {
        return metadata().getSubtype().toUpperCase();
    }

    @Value.Derived
    public String parameter() {
        return main().getSubtype().contains("+") ? main().getSubtype().substring(main().getSubtype().lastIndexOf("+")+1)  : main().getSubtype();
    }

    @Value.Default
    public int qs() {
        return 1000;
    }

    public boolean matches(MediaType mediaType) {
        return main().isCompatible(mediaType) || metadata().isCompatible(mediaType) || alternative().map(mediaType1 -> mediaType1.isCompatible(mediaType)).orElse(false);
    }

}
