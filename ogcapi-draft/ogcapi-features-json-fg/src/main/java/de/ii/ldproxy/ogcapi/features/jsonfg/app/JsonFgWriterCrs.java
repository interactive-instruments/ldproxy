/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Singleton
@AutoBind
public class JsonFgWriterCrs implements GeoJsonWriter {

    static public String JSON_KEY = "coordRefSys";

    boolean isEnabled;

    @Inject
    JsonFgWriterCrs() {
    }

    @Override
    public JsonFgWriterCrs create() {
        return new JsonFgWriterCrs();
    }

    @Override
    public int getSortPriority() {
        return 130;
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(context.encoding());

        if (isEnabled && context.encoding().isFeatureCollection()) {
            writeCrs(context.encoding().getJson(), context.encoding().getTargetCrs());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (isEnabled && !context.encoding().isFeatureCollection()) {
            writeCrs(context.encoding().getJson(), context.encoding().getTargetCrs());
        }

        // next chain for extensions
        next.accept(context);
    }

    private void writeCrs(JsonGenerator json, EpsgCrs crs) throws IOException {
        json.writeStringField(JSON_KEY, crs.toUriString());

        // TODO temporary additional member to support T17 clients, remove
        json.writeStringField("coord-ref-sys", crs.toUriString());
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> Objects.requireNonNullElse(cfg.getCoordRefSys(), false))
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.coordRefSys) ||
                                            transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
                                    .isPresent();
    }
}
