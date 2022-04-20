/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.custom.extensions.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesExtensionsConfiguration.Builder.class)
public interface FeaturesExtensionsConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    /**
     * @langEn Enables support for the POST HTTP method on the "Features" resource.
     * @langDe Aktiviert die Unterstützung für die HTTP-Methode POST auf der Ressource "Features"
     * @default `false`
     */
    @Nullable
    Boolean getPostOnItems();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean shouldSupportPostOnItems() {
        return Objects.equals(getPostOnItems(), true);
    }


    /**
     * @langEn Enables support for the `intersects` query parameter on the "Features" resource
     * @langDe Aktiviert die Unterstützung für den Query-Parameter `intersects` auf der Ressource "Features"
     * @default `false`
     */
    @Nullable
    Boolean getIntersectsParameter();

    @Override
    default Builder getBuilder() {
        return new ImmutableFeaturesExtensionsConfiguration.Builder();
    }
}
