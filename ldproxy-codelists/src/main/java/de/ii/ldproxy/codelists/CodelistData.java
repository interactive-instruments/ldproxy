/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCodelistData.Builder.class)
public interface CodelistData extends EntityData {

    enum IMPORT_TYPE {
        TEMPLATES,
        GML_DICTIONARY,
        ONEO_SCHLUESSELLISTE
    }

    abstract class Builder implements EntityDataBuilder<CodelistData> {
    }

    String getLabel();

    Map<String, String> getEntries();

    IMPORT_TYPE getSourceType();

    Optional<String> getSourceUrl();

    Optional<String> getFallback();
}
