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
import org.immutables.value.Value;

import java.util.List;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFilterTransformersConfiguration.Builder.class)

//TODO: also allow on global level (could we just use the same configuration there?)
public abstract class FilterTransformersConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return true;
    }

    public abstract List<FilterTransformerConfiguration> getTransformers();

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return new ImmutableFilterTransformersConfiguration.Builder()
                                                       .from(extensionConfigurationDefault)
                                                       .enabled(getEnabled())
                                                       .addAllTransformers(getTransformers())
                                                       .build();
    }
}