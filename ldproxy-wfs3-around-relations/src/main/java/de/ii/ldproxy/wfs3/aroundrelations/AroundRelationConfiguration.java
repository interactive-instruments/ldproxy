/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationExtension;
import de.ii.xsf.dropwizard.api.JacksonSubTypeIds;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableAroundRelationConfiguration.class)
public abstract class AroundRelationConfiguration implements FeatureTypeConfigurationExtension {

    public static final String EXTENSION_KEY = "aroundRelations";

    public abstract List<Relation> getRelations();

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


}
