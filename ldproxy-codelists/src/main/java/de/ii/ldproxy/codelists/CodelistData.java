/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.AbstractEntityData;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ImmutableCodelistData.class)
public abstract class CodelistData extends AbstractEntityData {

    public enum IMPORT_TYPE {
        TEMPLATES,
        GML_DICTIONARY,
        ONEO_SCHLUESSELLISTE
    }

    @Value.Default
    @Override
    public long getCreatedAt() {
        return Instant.now().toEpochMilli();
    }

    @Value.Default
    @Override
    public long getLastModified() {
        return Instant.now().toEpochMilli();
    }

    public abstract String getLabel();

    public abstract Map<String,String> getEntries();

    public abstract IMPORT_TYPE getSourceType();

    public abstract Optional<String> getSourceUrl();

    public abstract Optional<String> getFallback();
}
