/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableHtmlConfiguration.Builder.class)
public abstract class HtmlConfiguration implements ExtensionConfiguration {

    enum LAYOUT { CLASSIC, COMPLEX_OBJECTS }

    @Value.Default
    @Override
    public boolean getEnabled() {
        return true;
    }

    @Value.Default
    public boolean getNoIndexEnabled() { return true; }

    @Value.Default
    public LAYOUT getLayout() { return LAYOUT.CLASSIC; }
}
