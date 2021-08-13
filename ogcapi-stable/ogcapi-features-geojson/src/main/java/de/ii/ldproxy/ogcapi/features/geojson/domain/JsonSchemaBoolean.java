/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaBoolean;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaBoolean.class)
public abstract class JsonSchemaBoolean extends JsonSchema {

    public final String getType() { return "boolean"; }

}
