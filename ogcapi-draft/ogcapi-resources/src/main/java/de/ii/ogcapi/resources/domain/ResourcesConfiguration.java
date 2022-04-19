/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableResourcesConfiguration.Builder.class)
public interface ResourcesConfiguration extends ExtensionConfiguration, CachingConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    /**
     * @lang_en Controls whether the resources should be able to be created and deleted
     * via PUT and DELETE through the API.
     * @lang_de Steuert, ob die Ressourcen über PUT und DELETE über die API erzeugt und
     * gelöscht werden können sollen.
     * @default `false`
     */
    @Nullable
    Boolean getManagerEnabled();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isManagerEnabled() { return Objects.equals(getManagerEnabled(), true); }

    @Override
    default Builder getBuilder() {
        return new ImmutableResourcesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableResourcesConfiguration.Builder builder = ((ImmutableResourcesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        return builder.build();
    }
}