/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterCrs implements GeoJsonWriter {

    @Override
    public GeoJsonWriterCrs create() {
        return new GeoJsonWriterCrs();
    }

    @Override
    public int getSortPriority() {
        return 50;
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context,
                        Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.encoding().isFeatureCollection()) {
            writeCrs(context.encoding().getJson(), context.encoding().getCrsTransformer(), context.encoding().getDefaultCrs());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context,
                               Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (!context.encoding().isFeatureCollection()) {
            writeCrs(context.encoding().getJson(), context.encoding().getCrsTransformer(), context.encoding().getDefaultCrs());
        }

        // next chain for extensions
        next.accept(context);
    }

    private void writeCrs(JsonGenerator json, Optional<CrsTransformer> crsTransformer,
                          EpsgCrs defaultCrs) throws IOException {
        if (crsTransformer.isPresent() && !Objects.equals(crsTransformer.get()
                                                                        .getTargetCrs(), defaultCrs)) {
            json.writeStringField("crs", crsTransformer.get()
                                                       .getTargetCrs()
                                                       .toUriString());
        }
    }
}
