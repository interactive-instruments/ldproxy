/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMetadataDates.class)
public abstract class MetadataDates {

    public abstract Optional<String> getCreation();
    public abstract Optional<String> getPublication();
    public abstract Optional<String> getRevision();
    public abstract Optional<String> getValidTill();
    public abstract Optional<String> getReceivedOn();
}
