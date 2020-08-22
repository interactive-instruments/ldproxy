/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
    public void onStart(FeatureTransformationContextGeoJson transformationContext,
                        Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.isFeatureCollection()) {
            writeCrs(transformationContext.getJson(), transformationContext.getCrsTransformer(), transformationContext.getDefaultCrs());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!transformationContext.isFeatureCollection()) {
            writeCrs(transformationContext.getJson(), transformationContext.getCrsTransformer(), transformationContext.getDefaultCrs());
        }

        // next chain for extensions
        next.accept(transformationContext);
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
