/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.generalization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableGeneralizationConfiguration.class)
public abstract class GeneralizationConfiguration implements ExtensionConfiguration {
    public static final String EXTENSION_KEY = "generalizationExtension";
    public static final String EXTENSION_TYPE = "GENERALIZATION";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
