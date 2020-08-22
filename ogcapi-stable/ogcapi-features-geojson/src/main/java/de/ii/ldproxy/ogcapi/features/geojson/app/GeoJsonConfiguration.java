/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGeoJsonConfiguration.Builder.class)
public interface GeoJsonConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    FeatureTransformerGeoJson.NESTED_OBJECTS getNestedObjectStrategy();

    @Nullable
    FeatureTransformerGeoJson.MULTIPLICITY getMultiplicityStrategy();

    @Nullable
    Boolean getUseFormattedJsonOutput();

    @Nullable
    String getSeparator();

    Optional<JsonLdOptions> getJsonLd();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableJsonLdOptions.Builder.class)
    interface JsonLdOptions {

        String getContext();

        List<String> getTypes();

        Optional<String> getIdTemplate();
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableGeoJsonConfiguration.Builder();
    }
}
