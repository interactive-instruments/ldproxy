/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableJsonFgConfiguration.Builder.class)
public interface JsonFgConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    enum OPTION { describedby, featureTypes, when, where, refSys }

    @Nullable
    Boolean getWhen();

    @Nullable
    WhereConfiguration getWhere();

    @Nullable
    Boolean getDescribedby();

    @Nullable
    Boolean getRefSys();

    @Nullable
    List<String> getFeatureTypes();

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

        if (Objects.nonNull(getFeatureTypes()))
            builder.featureTypes(getFeatureTypes());
        else if (Objects.nonNull(src.getFeatureTypes()))
            builder.featureTypes(src.getFeatureTypes());

        return builder.build();
    }
}
