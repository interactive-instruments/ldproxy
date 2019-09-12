/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public interface FeatureTransformationContext {
    enum Event {
        START,
        END,
        FEATURE_START,
        FEATURE_END,
        PROPERTY,
        COORDINATES,
        GEOMETRY_END
    }

    OgcApiDatasetData getServiceData();

    String getCollectionName();

    OutputStream getOutputStream();

    Optional<CrsTransformer> getCrsTransformer();

    List<OgcApiLink> getLinks();

    boolean isFeatureCollection();

    @Value.Default
    default boolean isHitsOnly() {
        return false;
    }

    @Value.Default
    default boolean isHitsOnlyIfMore() {
        return false;
    }

    @Value.Default
    default boolean isPropertyOnly() {
        return false;
    }

    @Value.Default
    default List<String> getFields() {
        return ImmutableList.of("*");
    }

    OgcApiRequestContext getWfs3Request();

    int getLimit();

    int getOffset();

    @Value.Derived
    default int getPage() {
        return getLimit() > 0 ? (getLimit() + getOffset()) / getLimit() : 0;
    }

    @Nullable
    State getState();

    // to ValueTransformerContext
    @Value.Derived
    default String getServiceUrl() {
        return getWfs3Request().getUriCustomizer()
                               .copy()
                               .cutPathAfterSegments(getServiceData().getId())
                               .clearParameters()
                               .toString();
    }

    // to generalization module
    @Value.Default
    default double getMaxAllowableOffset() {
        return 0;
    }

    @Value.Default
    default boolean shouldSwapCoordinates() {
        return false;
    }

    @Value.Default
    default int getGeometryPrecision() {
        return 0;
    }

    //@Value.Modifiable
    abstract class State {
        public abstract Event getEvent();

        public abstract OptionalLong getNumberReturned();

        public abstract OptionalLong getNumberMatched();

        public abstract Optional<TargetMapping> getCurrentMapping();

        public abstract List<Integer> getCurrentMultiplicity();

        public abstract Optional<String> getCurrentValue();
    }
}
