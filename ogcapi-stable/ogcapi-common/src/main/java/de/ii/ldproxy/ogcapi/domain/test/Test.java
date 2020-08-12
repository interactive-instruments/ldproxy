/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain.test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableBuilder;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTest.Builder.class)
public interface Test extends Buildable<Test> {
    abstract class Builder implements BuildableBuilder<Test> {}

    String getId();

    String getLabel();

    String getDescription();

    @Override
    default ImmutableTest.Builder getBuilder() {
        return new ImmutableTest.Builder().from(this);
    }
}
