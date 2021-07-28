/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain.changes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableChangeContext.Builder.class)
public interface ChangeContext {

    enum Operation { INSERT, UPDATE, DELETE }

    @Value.Default
    default Instant getModified() { return Instant.now().truncatedTo(ChronoUnit.SECONDS); }

    OgcApiDataV2 getApiData();
    String getCollectionId();
    Operation getOperation();
    List<String> getFeatureIds();
    Optional<BoundingBox> getBoundingBox();
    Optional<TemporalExtent> getInterval();

}
