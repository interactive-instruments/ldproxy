/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import de.ii.ldproxy.ogcapi.collections.schema.domain.SchemaConfiguration;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.io.IOException;
import java.lang.annotation.Repeatable;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class GeoJsonWriterDescribedby implements GeoJsonWriter {

    private final I18n i18n;

    public GeoJsonWriterDescribedby(@Requires I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public GeoJsonWriterDescribedby create() {
        return new GeoJsonWriterDescribedby(i18n);
    }

    @Override
    public int getSortPriority() {
        // must be after the Links writer
        return 110;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled(transformationContext) && transformationContext.isFeatureCollection()) {
            String label = transformationContext.getApiData()
                                                .getCollections()
                                                .get(transformationContext.getCollectionId())
                                                .getLabel();
            transformationContext.getState().addCurrentFeatureCollectionLinks(new ImmutableLink.Builder().rel("describedby")
                                                                                                         .href(transformationContext.getServiceUrl() + "/collections/" + transformationContext.getCollectionId() + "/schemas/collection")
                                                                                                         .type("application/schema+json")
                                                                                                         .title(i18n.get("schemaLinkCollection", transformationContext.getLanguage()).replace("{{collection}}", label))
                                                                                                         .build());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled(transformationContext) && !transformationContext.isFeatureCollection()) {
            String label = transformationContext.getApiData()
                                                .getCollections()
                                                .get(transformationContext.getCollectionId())
                                                .getLabel();
            transformationContext.getState().addCurrentFeatureLinks(new ImmutableLink.Builder().rel("describedby")
                                                                                               .href(transformationContext.getServiceUrl() + "/collections/" + transformationContext.getCollectionId() + "/schemas/feature")
                                                                                               .type("application/schema+json")
                                                                                               .title(i18n.get("schemaLinkFeature", transformationContext.getLanguage()).replace("{{collection}}", label))
                                                                                               .build());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getCollections()
                                    .get(transformationContext.getCollectionId())
                                    .getExtension(JsonFgConfiguration.class)
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(JsonFgConfiguration::getDescribedby)
                                    .isPresent()
                && transformationContext.getApiData()
                                        .getCollections()
                                        .get(transformationContext.getCollectionId())
                                        .getExtension(SchemaConfiguration.class)
                                        .filter(SchemaConfiguration::isEnabled)
                                        .isPresent();
    }
}
