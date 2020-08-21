/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.collections.queryables.Queryable;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableStyleLayer.class)
public abstract class StyleLayer {

    public abstract String getId();

    public abstract Optional<String> getDescription();

    public abstract Optional<String> getType();

    public abstract Optional<List<Queryable>> getAttributes();

    public abstract Optional<Link> getSampleData();
}
