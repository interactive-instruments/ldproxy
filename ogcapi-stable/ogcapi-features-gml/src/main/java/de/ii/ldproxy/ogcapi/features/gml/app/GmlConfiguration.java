/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.gml.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableGmlConfiguration.Builder.class)
public interface GmlConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableGmlConfiguration.Builder();
    }
}
