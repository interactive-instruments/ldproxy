/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableJsonFgConfiguration.Builder.class)
public interface JsonFgConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    enum OPTION { describedby, featureType, when, where, coordRefSys, links }

    @Nullable
    Boolean getWhen();

    @Nullable
    WhereConfiguration getWhere();

    @Nullable
    Boolean getDescribedby();

    @Nullable
    Boolean getCoordRefSys();

    @Nullable
    List<String> getFeatureType();

    @Nullable
    List<Link> getLinks();

    List<OPTION> getIncludeInGeoJson();

    @Override
    default Builder getBuilder() {
        return new ImmutableJsonFgConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableJsonFgConfiguration.Builder builder = new ImmutableJsonFgConfiguration.Builder()
                .from(source)
                .from(this);

        ImmutableJsonFgConfiguration src = (ImmutableJsonFgConfiguration) source;

        if (Objects.nonNull(getWhere()) && Objects.nonNull(src.getWhere()))
            builder.where(getWhere().mergeInto(src.getWhere()));

        if (Objects.nonNull(getFeatureType()))
            builder.featureType(getFeatureType());
        else if (Objects.nonNull(src.getFeatureType()))
            builder.featureType(src.getFeatureType());

        return builder.build();
    }
}
