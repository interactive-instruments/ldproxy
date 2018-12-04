/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableWfs3ServiceMetadata.class)
public abstract class Wfs3ServiceMetadata {

    public abstract Optional<String> getContactName();

    public abstract Optional<String> getContactUrl();

    public abstract Optional<String> getContactEmail();

    public abstract Optional<String> getContactPhone();

    public abstract Optional<String> getLicenseName();

    public abstract Optional<String> getLicenseUrl();

    public abstract List<String> getKeywords();
}
