/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableWfs3Collection.class)
public abstract class Wfs3Collection {

    public abstract String getId();

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getDescription();

    public abstract Wfs3Extent getExtent();

    public abstract List<Wfs3Link> getLinks();

    public abstract List<String> getCrs();

    //@JsonIgnore
    //public abstract String getPrefixedName();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
