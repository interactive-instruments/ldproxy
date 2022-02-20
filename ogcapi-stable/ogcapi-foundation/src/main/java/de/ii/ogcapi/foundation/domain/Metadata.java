/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableMetadata.Builder.class)
public interface Metadata {

    Optional<String> getContactName();

    Optional<String> getContactUrl();

    Optional<String> getContactEmail();

    Optional<String> getContactPhone();

    Optional<String> getCreatorName();

    Optional<String> getCreatorUrl();

    Optional<String> getCreatorLogoUrl();

    Optional<String> getPublisherName();

    Optional<String> getPublisherUrl();

    Optional<String> getPublisherLogoUrl();

    Optional<String> getLicenseName();

    Optional<String> getLicenseUrl();

    List<String> getKeywords();

    Optional<String> getVersion();

    Optional<String> getAttribution();
}
