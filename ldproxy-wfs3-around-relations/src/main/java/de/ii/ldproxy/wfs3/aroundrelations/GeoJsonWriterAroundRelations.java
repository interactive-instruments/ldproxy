/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.aroundrelations;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.target.geojson.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.target.geojson.GeoJsonWriter;
import de.ii.ldproxy.target.geojson.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.xtraplatform.akka.http.AkkaHttp;
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
public class GeoJsonWriterAroundRelations implements GeoJsonWriter {

    //TODO: either publish factories and create new instance every time and remove reset again
    //TODO: or move all state to transformationcontext and make writers stateless

    @Override
    public GeoJsonWriterAroundRelations create() {
        return new GeoJsonWriterAroundRelations(new SimpleAroundRelationResolver(akkaHttp));
    }

    public GeoJsonWriterAroundRelations create(AroundRelationResolver aroundRelationResolver) {
        return new GeoJsonWriterAroundRelations(aroundRelationResolver);
    }

    private AroundRelationsQuery aroundRelationsQuery;
    private ImmutableList.Builder<Wfs3Link> links;

    //TODO inject, multiple implementations
    private final AroundRelationResolver aroundRelationResolver;

    @Requires
    AkkaHttp akkaHttp;

    public GeoJsonWriterAroundRelations() {
        this.aroundRelationResolver = null;
    }

    private GeoJsonWriterAroundRelations(AroundRelationResolver aroundRelationResolver) {
        this.aroundRelationResolver = aroundRelationResolver;
    }

    /*private GeoJsonWriterAroundRelations(AroundRelationResolver aroundRelationResolver) {
        this.aroundRelationResolver = aroundRelationResolver;
    }

    static GeoJsonWriterAroundRelations create(AroundRelationResolver aroundRelationResolver) {
        return new GeoJsonWriterAroundRelations(aroundRelationResolver);
    }*/

    @Override
    public int getSortPriority() {
        return 15;
    }

    private void reset() {
        this.aroundRelationsQuery = null;
        this.links = null;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        reset();

        this.aroundRelationsQuery = new AroundRelationsQuery(transformationContext);

        next.accept(transformationContext);
    }

    @Override
    public void onEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        this.aroundRelationsQuery = null;

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        // if resolve=false, clear context links

        FeatureTransformationContextGeoJson nextTransformationContext = transformationContext;

        if (aroundRelationsQuery.isActive() && !aroundRelationsQuery.isResolve()) {
            this.links = ImmutableList.<Wfs3Link>builder().addAll(transformationContext.getLinks());

            nextTransformationContext = ImmutableFeatureTransformationContextGeoJson.builder()
                                                                                    .from(transformationContext)
                                                                                    .links(ImmutableList.of())
                                                                                    .build();
        }

        next.accept(nextTransformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        // send request with bbox
        // if resolve=false, write links (parse response, extract)
        // if resolve=true, write additionalFeatures (pass through response)


        FeatureTransformationContextGeoJson nextTransformationContext = transformationContext;

        if (aroundRelationsQuery.isReady()) {

            if (aroundRelationsQuery.isResolve()) {
                next.accept(transformationContext);
            }

            boolean[] started = {false};

            aroundRelationsQuery.getQueries()
                                .forEach(consumerMayThrow(query -> {

                                    if (aroundRelationsQuery.isResolve()) {
                                        String resolved = aroundRelationResolver.resolve(query, "&f=json");

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
                                        String url = aroundRelationResolver.getUrl(query, "&f=json");

                                        links.add(ImmutableWfs3Link.builder()
                                                                   .href(url)
                                                                   .rel(query.configuration.getId())
                                                                   .type(query.configuration.getResponseType())
                                                                   .description(query.configuration.getLabel())
                                                                   .build());
                                    }
                                }));

            if (!aroundRelationsQuery.isResolve()) {
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
    public void onCoordinates(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (aroundRelationsQuery.isActive()) {
            transformationContext.getState()
                                 .getCurrentValue()
                                 .ifPresent(consumerMayThrow(coordinates -> {
                                     aroundRelationsQuery.addCoordinates(coordinates, transformationContext.getState()
                                                                                                           .getCoordinatesWriterBuilder()
                                                                                                           .get());
                                 }));
        }

        next.accept(transformationContext);
    }

    @Override
    public void onGeometryEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (aroundRelationsQuery.isActive()) {

            transformationContext.getState()
                                 .getCurrentGeometryType()
                                 .ifPresent(geo_json_geometry_type -> {
                                     aroundRelationsQuery.computeBbox(geo_json_geometry_type.toSimpleFeatureGeometry());
                                 });
        }

        next.accept(transformationContext);
    }

}
