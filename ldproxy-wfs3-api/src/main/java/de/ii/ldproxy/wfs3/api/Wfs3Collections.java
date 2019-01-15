/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableWfs3Collections.class)
public abstract class Wfs3Collections {

    public abstract List<Wfs3Link> getLinks();

    public abstract List<String> getCrs();

    public abstract List<Wfs3Collection> getCollections();

    public abstract List<Wfs3Style> getStyles();


}
