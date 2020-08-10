/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.entity.api.maptobuilder.BuildableBuilder;
import org.immutables.value.Value;

import java.util.List;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFilterTransformersConfiguration.Builder.class)

public interface FilterTransformersConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<FilterTransformerConfiguration> getTransformers();

    @Override
    default Builder getBuilder() {
        return new ImmutableFilterTransformersConfiguration.Builder();
    }
}
