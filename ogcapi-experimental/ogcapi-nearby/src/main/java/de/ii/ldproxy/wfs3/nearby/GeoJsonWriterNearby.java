/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.nearby;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.target.geojson.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.target.geojson.GeoJsonWriter;
import de.ii.ldproxy.target.geojson.ImmutableFeatureTransformationContextGeoJson;
import de.ii.xtraplatform.akka.http.Http;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterNearby implements GeoJsonWriter {

    //TODO: either publish factories and create new instance every time and remove reset again
    //TODO: or move all state to transformationcontext and make writers stateless

    @Override
    public GeoJsonWriterNearby create() {
        return new GeoJsonWriterNearby(new SimpleNearbyResolver(http.getDefaultClient()));
    }

    public GeoJsonWriterNearby create(NearbyResolver nearbyResolver) {
        return new GeoJsonWriterNearby(nearbyResolver);
    }

    private NearbyQuery nearbyQuery;
    private ImmutableList.Builder<OgcApiLink> links;

    //TODO inject, multiple implementations
    private final NearbyResolver nearbyResolver;

    @Requires
    Http http;

    public GeoJsonWriterNearby() {
        this.nearbyResolver = null;
    }

    private GeoJsonWriterNearby(NearbyResolver nearbyResolver) {
        this.nearbyResolver = nearbyResolver;
    }

    /*private GeoJsonWriterNearby(NearbyResolver nearbyResolver) {
        this.nearbyResolver = nearbyResolver;
    }

    static GeoJsonWriterNearby create(NearbyResolver nearbyResolver) {
        return new GeoJsonWriterNearby(nearbyResolver);
    }*/

    @Override
    public int getSortPriority() {
        return 15;
    }

    private void reset() {
        this.nearbyQuery = null;
        this.links = null;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext,
                        Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        reset();

        this.nearbyQuery = new NearbyQuery(transformationContext);

        next.accept(transformationContext);
    }

    @Override
    public void onEnd(FeatureTransformationContextGeoJson transformationContext,
                      Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        this.nearbyQuery = null;

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        // if resolve=false, clear context links

        FeatureTransformationContextGeoJson nextTransformationContext = transformationContext;

        if (nearbyQuery.isActive() && !nearbyQuery.isResolve()) {
            this.links = ImmutableList.<OgcApiLink>builder().addAll(transformationContext.getLinks());

            nextTransformationContext = ImmutableFeatureTransformationContextGeoJson.builder()
                                                                                    .from(transformationContext)
                                                                                    .links(ImmutableList.of())
                                                                                    .build();
        }

        next.accept(nextTransformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext,
                             Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        // send request with bbox
        // if resolve=false, write links (parse response, extract)
        // if resolve=true, write additionalFeatures (pass through response)


        FeatureTransformationContextGeoJson nextTransformationContext = transformationContext;

        if (nearbyQuery.isReady()) {

            if (nearbyQuery.isResolve()) {
                next.accept(transformationContext);
            }

            boolean[] started = {false};

            nearbyQuery.getQueries()
                                .forEach(consumerMayThrow(query -> {

                                    if (nearbyQuery.isResolve()) {
                                        String resolved = nearbyResolver.resolve(query, "&f=json");

                                        if (Objects.nonNull(resolved)) {

                                            if (!started[0]) {
                                                started[0] = true;

                                                transformationContext.getJson()
                                                                     .writeFieldName("additionalFeatures");
                                                transformationContext.getJson()
                                                                     .writeStartObject();
                                            }

                                            transformationContext.getJson()
                                                                 .writeFieldName(query.configuration.getId());
                                            transformationContext.getJson()
                                                                 .writeRawValue(resolved);
                                        }
                                    } else {
                                        String url = nearbyResolver.getUrl(query, "&f=json");

                                        links.add(new ImmutableOgcApiLink.Builder()
                                                .href(url)
                                                .rel(query.configuration.getId())
                                                .type(query.configuration.getResponseType())
                                                .title(query.configuration.getLabel())
                                                .build());
                                    }
                                }));

            if (!nearbyQuery.isResolve()) {
                nextTransformationContext = ImmutableFeatureTransformationContextGeoJson.builder()
                                                                                        .from(transformationContext)
                                                                                        .links(links.build())
                                                                                        .build();
                next.accept(nextTransformationContext);
            } else if (started[0]) {
                transformationContext.getJson()
                                     .writeEndObject();
            }

            this.links = null;
        } else {
            next.accept(transformationContext);
        }
    }

    @Override
    public void onCoordinates(FeatureTransformationContextGeoJson transformationContext,
                              Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (nearbyQuery.isActive()) {
            transformationContext.getState()
                                 .getCurrentValue()
                                 .ifPresent(consumerMayThrow(coordinates -> {
                                     nearbyQuery.addCoordinates(coordinates, transformationContext.getState()
                                                                                                           .getCoordinatesWriterBuilder()
                                                                                                           .get());
                                 }));
        }

        next.accept(transformationContext);
    }

    @Override
    public void onGeometryEnd(FeatureTransformationContextGeoJson transformationContext,
                              Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (nearbyQuery.isActive()) {

            transformationContext.getState()
                                 .getCurrentGeometryType()
                                 .ifPresent(geo_json_geometry_type -> {
                                     nearbyQuery.computeBbox(geo_json_geometry_type.toSimpleFeatureGeometry());
                                 });
        }

        next.accept(transformationContext);
    }

}
