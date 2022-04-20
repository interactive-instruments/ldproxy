/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.sorting.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableSortingConfiguration.Builder.class)
public interface SortingConfiguration extends ExtensionConfiguration {

    /**
     * @langEn Controls which of the attributes in queries can be used for sorting data.
     * Only direct attributes of the data types `STRING`, `DATETIME`, `INTEGER` and `FLOAT`
     * are allowed (no attributes from arrays or embedded objects). A current limitation is
     * that all attributes must have unique values, see
     * [Issue 488](https://github.com/interactive-instruments/ldproxy/issues/488).
     * @langDe Steuert, welche der Attribute in Queries für die Sortierung
     * von Daten verwendet werden können. Erlaubt sind nur direkte Attribute
     * (keine Attribute aus Arrays oder eingebetteten Objekten) der Datentypen `STRING`,
     * `DATETIME`, `INTEGER` und `FLOAT`.
     * @default `{}`
     */
    List<String> getSortables();

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableSortingConfiguration.Builder();
    }
}
