/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerServiceData;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Deprecated
@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiDataV1.Builder.class)
public abstract class OgcApiDataV1 extends FeatureTransformerServiceData<FeatureTypeConfigurationOgcApi> implements ExtendableConfiguration, ServiceData {

    static abstract class Builder implements EntityDataBuilder<OgcApiDataV1> {
    }

    @Override
    public long getEntitySchemaVersion() {
        return 1;
    }

    @Value.Default
    @Override
    public String getServiceType() {
        return "WFS3";
    }

    @Value.Default
    @Override
    public String getLabel() {
        return getId();
    }

    //behaves exactly like Map<String, FeatureTypeConfigurationOgcApi>, but supports mergeable builder deserialization
    //(immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    @Override
    public abstract ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> getFeatureTypes();

    public abstract List<EpsgCrs> getAdditionalCrs();

    //TODO: Optional does not work with nested builders
    @Nullable
    public abstract Metadata getMetadata();

    @Override
    @Value.Derived
    public boolean isLoading() {
        return Objects.nonNull(getFeatureProvider().getMappingStatus()) && getFeatureProvider().getMappingStatus()
                                                                                               .getLoading();
    }

    @Override
    @Value.Derived
    public boolean hasError() {
        return Objects.nonNull(getFeatureProvider().getMappingStatus())
                && getFeatureProvider().getMappingStatus()
                                       .getEnabled()
                && !getFeatureProvider().getMappingStatus()
                                        .getSupported()
                && Objects.nonNull(getFeatureProvider().getMappingStatus()
                                                       .getErrorMessage());
    }
}
