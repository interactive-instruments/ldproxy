/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableCityJsonConfiguration.Builder.class)
public interface CityJsonConfiguration extends ExtensionConfiguration, PropertyTransformations {

    enum Version {

        V10("1.0"), V11("1.1");

        private final String text;

        Version(String value) {
            text = value;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * @langEn Enables support for CityJSON text sequences (media type `application/city+json-seq`).
     * Requires version 1.1 or later.
     * @langDe Aktivitiert die Unterstützung für CityJSON Text Sequences (Media-Type `application/city+json-seq`).
     * Erfordert mindestens Version 1.1.
     * @default `false`
     */
    Optional<Boolean> getTextSequences();

    /**
     * @langEn Select the CityJSON version that should be returned. Supported versions are `V10` (CityJSON 1.0) and `V11` (CityJSON 1.1).
     * @langDe Wählen Sie die CityJSON-Version, die zurückgegeben werden soll. Unterstützte Versionen sind `V10` (CityJSON 1.0) und `V11` (CityJSON 1.1).
     * @default `V11`
     */
    Optional<Version> getVersion();

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableCityJsonConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        return new ImmutableCityJsonConfiguration.Builder()
            .from(source)
            .from(this)
            .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations())
            .build();
    }
}
