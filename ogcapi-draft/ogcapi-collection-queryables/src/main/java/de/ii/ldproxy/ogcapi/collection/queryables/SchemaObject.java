/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import java.util.ArrayList;
import java.util.List;

public class SchemaObject extends SchemaBase {
    public List<SchemaProperty> properties = new ArrayList<>();

    public SchemaProperty get(String name) {
        return (SchemaProperty) properties.stream()
                .filter(prop -> prop.path.equals(name))
                .findFirst()
                .orElse(null);
    }
}
