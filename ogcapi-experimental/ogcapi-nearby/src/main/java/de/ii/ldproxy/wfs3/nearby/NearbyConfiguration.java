/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.nearby;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import java.util.List;
import java.util.OptionalDouble;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableNearbyConfiguration.Builder.class)
public abstract class NearbyConfiguration implements ExtensionConfiguration {

    // TODO migrate existing AROUND_RELATIONS configs

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    @JsonMerge(value = OptBoolean.FALSE)
    @Value.Default
    public List<Relation> getRelations() { return ImmutableList.of(); };

    @Value.Immutable
    @Value.Modifiable
    @JsonDeserialize(as = ModifiableRelation.class)
    public static abstract class Relation {

        public abstract String getId();

        public abstract String getLabel();

        public abstract String getResponseType();

        public abstract String getUrlTemplate();

        public abstract OptionalDouble getBufferInMeters();
    }

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return new ImmutableNearbyConfiguration.Builder()
                                                    .from(extensionConfigurationDefault)
                                                    .from(this)
                                                    .build();
    }

}
