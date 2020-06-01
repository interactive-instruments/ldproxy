/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class SchemaObject extends SchemaBase {
    public List<SchemaProperty> properties = new ArrayList<>();
    public List<SchemaProperty> patternProperties = new ArrayList<>();

    public SchemaProperty get(String name) {
        return properties.stream()
                .filter(prop -> prop.path.equals(name))
                .findFirst()
                .orElse(null);
    }

    public SchemaProperty getPattern(String name) {
        return patternProperties.stream()
                .filter(prop -> prop.path.equals(name))
                .findFirst()
                .orElse(null);
    }
}
