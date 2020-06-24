/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGeoJsonConfiguration.Builder.class)
public abstract class GeoJsonConfiguration implements ExtensionConfiguration, FeatureTransformations {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    public abstract Optional<JsonLdOptions> getJsonLd();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableJsonLdOptions.Builder.class)
    public interface JsonLdOptions {

        String getContext();

        List<String> getTypes();

        Optional<String> getIdTemplate();
    }

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return new ImmutableGeoJsonConfiguration.Builder()
                .from(extensionConfigurationDefault)
                .enabled(getEnabled())
                .jsonLd(getJsonLd())
                .build();
    }
}
