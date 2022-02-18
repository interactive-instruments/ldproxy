/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import dagger.Lazy;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class GeoJsonWriterRegistryImpl implements GeoJsonWriterRegistry {

    private final Lazy<Set<GeoJsonWriter>> geoJsonWriters;

    @Inject
    public GeoJsonWriterRegistryImpl(Lazy<Set<GeoJsonWriter>> geoJsonWriters) {
        this.geoJsonWriters = geoJsonWriters;
    }

    @Override
    public List<GeoJsonWriter> getGeoJsonWriters() {
        return ImmutableList.copyOf(geoJsonWriters.get());
    }
}
