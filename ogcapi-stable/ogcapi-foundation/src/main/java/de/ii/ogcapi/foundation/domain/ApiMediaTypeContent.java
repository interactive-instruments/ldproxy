/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import io.swagger.v3.oas.models.media.Schema;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface ApiMediaTypeContent {
        String getSchemaRef();
        Schema getSchema();
        List<Example> getExamples();
        ApiMediaType getOgcApiMediaType();
}
