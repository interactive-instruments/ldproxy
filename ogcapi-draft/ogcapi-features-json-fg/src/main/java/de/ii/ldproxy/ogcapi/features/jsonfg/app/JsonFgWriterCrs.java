/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.FeatureTransformationContextJsonFgExtension;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterCrs implements GeoJsonWriter {

    boolean isEnabled;

    @Override
    public JsonFgWriterCrs create() {
        return new JsonFgWriterCrs();
    }

    @Override
    public int getSortPriority() {
        return 130;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext,
                        Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(transformationContext);

        if (isEnabled && transformationContext.isFeatureCollection()) {
            FeatureTransformationContextJsonFgExtension jsonfg = (FeatureTransformationContextJsonFgExtension) transformationContext.getExtensions().get("jsonfg");
            if (Objects.nonNull(jsonfg)) {
                writeCrs(transformationContext.getJson(), jsonfg.getCrs());
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled && !transformationContext.isFeatureCollection()) {
            FeatureTransformationContextJsonFgExtension jsonfg = (FeatureTransformationContextJsonFgExtension) transformationContext.getExtensions().get("jsonfg");
            if (Objects.nonNull(jsonfg)) {
                writeCrs(transformationContext.getJson(), jsonfg.getCrs());
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    private void writeCrs(JsonGenerator json, EpsgCrs crs) throws IOException {
        json.writeStringField("coord-ref-sys", crs.toUriString());
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> Objects.requireNonNullElse(cfg.getRefSys(),false))
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.refSys) ||
                                            transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
                                    .isPresent();
    }
}
