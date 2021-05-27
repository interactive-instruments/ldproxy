/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.List;
import java.util.Optional;

public abstract class Metadata2 extends PageRepresentation {

    public abstract List<String> getKeywords();

    public abstract Optional<String> getPointOfContact();

    public abstract Optional<String> getAccessConstraints();

    public abstract Optional<MetadataDates> getDates();

    public abstract Optional<String> getVersion();

    public abstract Optional<String> getScope();

}
