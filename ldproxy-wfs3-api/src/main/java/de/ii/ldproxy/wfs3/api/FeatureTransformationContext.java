/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
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

    Wfs3ServiceData getServiceData();

    String getCollectionName();

    OutputStream getOutputStream();

    Optional<CrsTransformer> getCrsTransformer();

    List<Wfs3Link> getLinks();

    boolean isFeatureCollection();

    Wfs3RequestContext getWfs3Request();

    int getLimit();

    int getOffset();

    @Value.Derived
    default int getPage() {
        return getLimit() > 0 ? (getLimit() + getOffset()) / getLimit() : 0;
    }

    @Nullable
    State getState();

    // to ValueTransformerContext
    String getServiceUrl();

    // to generalization module
    double getMaxAllowableOffset();

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
