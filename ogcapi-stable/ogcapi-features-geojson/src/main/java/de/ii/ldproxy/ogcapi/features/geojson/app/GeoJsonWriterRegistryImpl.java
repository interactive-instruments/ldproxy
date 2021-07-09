/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class GeoJsonWriterRegistryImpl implements GeoJsonWriterRegistry {

    private final BundleContext bundleContext;
    private final List<GeoJsonWriter> geoJsonWriters;

    public GeoJsonWriterRegistryImpl(@Context BundleContext bundleContext) {
        this.geoJsonWriters = new ArrayList<>();
        this.bundleContext = bundleContext;
    }

    @Override
    public List<GeoJsonWriter> getGeoJsonWriters() {
        return geoJsonWriters;
    }

    private synchronized void onArrival(ServiceReference<GeoJsonWriter> ref) {
        try {
            final GeoJsonWriter geoJsonWriter = bundleContext.getService(ref);

            geoJsonWriters.add(geoJsonWriter);
        } catch (Throwable e) {
            //LOGGER.error("E", e);
        }
    }

    private synchronized void onDeparture(ServiceReference<GeoJsonWriter> ref) {
        final GeoJsonWriter geoJsonWriter = bundleContext.getService(ref);

        if (Objects.nonNull(geoJsonWriter)) {

            geoJsonWriters.remove(geoJsonWriter);
        }
    }
}
