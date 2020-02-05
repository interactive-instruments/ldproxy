/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
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

    OgcApiApiDataV2 getApiData();

    String getCollectionId();

    OutputStream getOutputStream();

    Optional<CrsTransformer> getCrsTransformer();

    EpsgCrs getDefaultCrs();

    List<OgcApiLink> getLinks();

    boolean isFeatureCollection();

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

    OgcApiRequestContext getOgcApiRequest();

    int getLimit();

    int getOffset();

    @Value.Derived
    default int getPage() {
        return getLimit() > 0 ? (getLimit() + getOffset()) / getLimit() : 0;
    }

    @Value.Default
    default Optional<Locale> getLanguage() { return getOgcApiRequest().getLanguage(); }

    Optional<I18n> getI18n();

    @Nullable
    State getState();

    // to ValueTransformerContext
    @Value.Derived
    default String getServiceUrl() {
        if (getApiData().getApiVersion()
                        .isPresent()) {
            return getOgcApiRequest().getUriCustomizer()
                                     .copy()
                                     .cutPathAfterSegments(getApiData().getId(), String.format("v%d", getApiData().getApiVersion()
                                                                                                                  .get()))
                                     .clearParameters()
                                     .toString();
        }
        return getOgcApiRequest().getUriCustomizer()
                                 .copy()
                                 .cutPathAfterSegments(getApiData().getId())
                                 .clearParameters()
                                 .toString();
    }

    // TODO: to generalization module
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
