/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.features.domain.FeatureTypeConfiguration;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * # Collection
 * @langEn Every collection corresponds to a feature type defined in the feature provider
 * (only *Feature Collections* are currently supported).
 * @langDe Jedes Collection-Objekt beschreibt eine Objektart aus einem Feature Provider
 * (derzeit werden nur Feature Collections von ldproxy unterstützt). Es setzt sich aus den
 * folgenden Eigenschaften zusammen:
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableFeatureTypeConfigurationOgcApi.Builder.class)
public interface FeatureTypeConfigurationOgcApi extends FeatureTypeConfiguration, ExtendableConfiguration, Buildable<FeatureTypeConfigurationOgcApi> {

    abstract class Builder implements BuildableBuilder<FeatureTypeConfigurationOgcApi> {

        // jackson should append to instead of replacing extensions
        @JsonIgnore
        public abstract Builder extensions(Iterable<? extends ExtensionConfiguration> elements);

        @JsonProperty("api")
        public abstract Builder addAllExtensions(Iterable<? extends ExtensionConfiguration> elements);

    }

    @Override
    default ImmutableFeatureTypeConfigurationOgcApi.Builder getBuilder() {
        return new ImmutableFeatureTypeConfigurationOgcApi.Builder().from(this);
    }

    @Value.Default
    default boolean getEnabled() {
        return true;
    }


    /**
     * @langEn The *Feature* resource defines a unique URI for every feature, but this URI is
     * only stable as long as the API URI stays the same. For use cases where external
     * persistent feature URIs, which redirect to the current API URI, are used, this option
     * allows to use such URIs as canonical URI of every feature. To enable this option,
     * provide an URI template where `{{value}}` is replaced with the feature id.
     * @langDe Über die Feature-Ressource hat jedes Feature zwar eine feste URI, die für Links
     * verwendet werden kann, allerdings ist die URI nur so lange stabil, wie die API stabil bleibt.
     * Um von Veränderungen in der URI unabhängig zu sein, kann es sinnvoll oder gewünscht sein,
     * API-unabhängige URIs für die Features zu definieren und von diesen URIs auf die jeweils
     * gültige API-URI weiterzuleiten. Diese kananosche URI kann auch in ldproxy Konfiguriert
     * und bei den Features kodiert werden. Hierfür ist ein Muster der Feature-URI anzugeben,
     * wobei `{{value}}` als Ersetzungspunkt für den lokalen Identifikator des Features in der
     * API angegeben werden kann.
     * @default `null`
     */
    Optional<String> getPersistentUriTemplate();

    Optional<CollectionExtent> getExtent();

    /**
     * @langEn Array of additional link objects, required keys are `href` (the URI), `label` and `rel` (the relation).
     * @langDe Erlaubt es, zusätzliche Links bei jeder Objektart zu ergänzen. Der Wert ist ein Array von
     * Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`), der anzuzeigende Text
     * (`label`) und die Link-Relation (`rel`).
     * @default `[]
     */
    List<Link> getAdditionalLinks();

    @JsonProperty(value = "api")
    @JsonAlias(value = "capabilities")
    @Override
    List<ExtensionConfiguration> getExtensions();

    @JsonIgnore
    List<ExtensionConfiguration> getParentExtensions();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default List<ExtensionConfiguration> getMergedExtensions() {
        return getMergedExtensions(Lists.newArrayList(Iterables.concat(getParentExtensions(), getExtensions())));
    }

    @Value.Check
    default FeatureTypeConfigurationOgcApi mergeBuildingBlocks() {
        List<ExtensionConfiguration> distinctExtensions = getMergedExtensions(getExtensions());

        // remove duplicates
        if (getExtensions().size() > distinctExtensions.size()) {
            return new ImmutableFeatureTypeConfigurationOgcApi.Builder().from(this)
                                                      .extensions(distinctExtensions)
                                                      .build();
        }

        return this;
    }
}

