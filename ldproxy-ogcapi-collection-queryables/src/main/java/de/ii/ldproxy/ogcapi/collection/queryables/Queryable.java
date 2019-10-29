/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(as = ImmutableQueryable.class)
public abstract class Queryable {

    public abstract String getId();
    public abstract String getType();
    public abstract Optional<String> getTitle();
    public abstract Optional<String> getDescription();
    public abstract Optional<Boolean> getRequired();
    public abstract Optional<List<String>> getMediaTypes();

    public abstract Optional<String> getPattern();
    public abstract Optional<List<Object>> getRange();
    public abstract Optional<List<String>> getValues();

}
