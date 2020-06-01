/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.Optional;

@Deprecated
public class SchemaProperty extends SchemaBase {
    public String path = null;
    public Optional<String> literalType = Optional.empty();
    public Optional<String> wellknownType = Optional.empty();
    public Optional<SchemaObject> objectType = Optional.empty();
    public Integer minItems = 0;
    public Integer maxItems = 1;
}
