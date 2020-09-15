/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureType;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.util.*;

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

    OgcApiDataV2 getApiData();

    String getCollectionId();

    Optional<FeatureSchema> getFeatureSchema();

    OutputStream getOutputStream();

    Optional<CrsTransformer> getCrsTransformer();

    EpsgCrs getDefaultCrs();

    List<Link> getLinks();

    boolean isFeatureCollection();

    Map<String, Codelist> getCodelists();

    @Value.Default
    default boolean getShowsFeatureSelfLink() {
        return true;
    }

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

    ApiRequestContext getOgcApiRequest();

    int getLimit();

    int getOffset();

    @Value.Derived
    default int getPage() {
        return getLimit() > 0 ? (getLimit() + getOffset()) / getLimit() : 0;
    }

    Optional<Locale> getLanguage();

    Optional<I18n> getI18n();

    @Nullable
    State getState();

    // to ValueTransformerContext
    @Value.Derived
    default String getServiceUrl() {
        return getOgcApiRequest().getUriCustomizer()
                                 .copy()
                                 .cutPathAfterSegments(getApiData().getSubPath().toArray(new String[0]))
                                 .clearParameters()
                                 .toString();
    }

    // TODO: to geometry simplification module
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

    abstract class State {
        public abstract Event getEvent();

        public abstract OptionalLong getNumberReturned();

        public abstract OptionalLong getNumberMatched();

        public abstract Optional<FeatureType> getCurrentFeatureType();

        public abstract Optional<FeatureProperty> getCurrentFeatureProperty();

        public abstract List<Integer> getCurrentMultiplicity();

        public abstract Optional<String> getCurrentValue();
    }
}
