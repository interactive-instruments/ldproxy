/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSubTypes({
        @JsonSubTypes.Type(value = QueryableBoolean.class, name = "QueryableBoolean"),
        @JsonSubTypes.Type(value = QueryableDate.class, name = "QueryableDate"),
        @JsonSubTypes.Type(value = QueryableDateTime.class, name = "QueryableDateTime"),
        @JsonSubTypes.Type(value = QueryableEnum.class, name = "QueryableEnum"),
        @JsonSubTypes.Type(value = QueryableNumber.class, name = "QueryableNumber"),
        @JsonSubTypes.Type(value = QueryableString.class, name = "QueryableString")
})
@JsonDeserialize(as = ImmutableQueryable.class)
public abstract class Queryable {

    public abstract String getId();
    public abstract String getType();
    public abstract Optional<String> getTitle();
    public abstract Optional<String> getDescription();
    public abstract Optional<Boolean> getRequired();
    public abstract Optional<List<String>> getMediaTypes();

}
