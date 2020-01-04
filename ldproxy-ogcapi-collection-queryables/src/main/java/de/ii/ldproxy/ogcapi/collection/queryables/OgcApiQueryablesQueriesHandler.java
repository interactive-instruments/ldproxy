/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.target.geojson.GeoJsonPropertyMapping;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.target.geojson.GeoJsonMapping.GEO_JSON_TYPE;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueryablesQueriesHandler.class})
public class OgcApiQueryablesQueriesHandler implements OgcApiQueriesHandler<OgcApiQueryablesQueriesHandler.Query> {

    @Requires
    I18n i18n;

    public enum Query implements OgcApiQueryIdentifier {QUERYABLES}

    @Value.Immutable
    public interface OgcApiQueryInputQueryables extends OgcApiQueryInput {
        String getCollectionId();
        boolean getIncludeHomeLink();
        boolean getIncludeLinkHeader();
    }

    private final OgcApiExtensionRegistry extensionRegistry;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;

    public OgcApiQueryablesQueriesHandler(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;

        this.queryHandlers = ImmutableMap.of(
                Query.QUERYABLES, OgcApiQueryHandler.with(OgcApiQueryInputQueryables.class, this::getQueryablesResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void checkCollectionId(OgcApiDatasetData apiData, String collectionId) {
        if (!apiData.isFeatureTypeEnabled(collectionId)) {
            throw new NotFoundException();
        }
    }

    private Response getQueryablesResponse(OgcApiQueryInputQueryables queryInput, OgcApiRequestContext requestContext) {

        OgcApiDataset api = requestContext.getApi();
        OgcApiDatasetData apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isFeatureTypeEnabled(collectionId))
            throw new NotFoundException();

        OgcApiQueryablesFormatExtension outputFormat = api.getOutputFormat(
                    OgcApiQueryablesFormatExtension.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/queryables")
                .orElseThrow(NotAcceptableException::new);

        checkCollectionId(api.getData(), collectionId);
        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<OgcApiLink> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        ImmutableQueryables.Builder queryables = ImmutableQueryables.builder();

        FeatureTypeMapping featureTypeMapping = apiData.getFeatureProvider()
                .getMappings()
                .get(collectionId);
        ValueBuilderMap<SourcePathMapping, ImmutableSourcePathMapping.Builder> dbg1 = featureTypeMapping.getMappings();
        Map<List<String>, SourcePathMapping> dbg2 = featureTypeMapping.getMappingsWithPathAsList();

        dbg1.forEach((key2,value2) -> {
            TargetMapping dbg3 = value2.getMappingForType("general");
            if (dbg3 instanceof OgcApiFeaturesGenericMapping) {
                OgcApiFeaturesGenericMapping dbg4 = (OgcApiFeaturesGenericMapping) dbg3;
                if (dbg4.isFilterable()) {
                    if (dbg4.isValue() || dbg4.isId()) {
                        TargetMapping dbg5 = value2.getMappingForType("application/geo+json");
                        if (dbg5 instanceof GeoJsonPropertyMapping) {
                            GeoJsonPropertyMapping dbg6 = (GeoJsonPropertyMapping) dbg5;
                            GEO_JSON_TYPE type = dbg6.getType();
                            switch (type.toString()) {
                                case ("BOOLEAN"):
                                    queryables.addQueryables(ImmutableQueryable.builder()
                                            .id(dbg4.getName())
                                            .type("boolean")
                                            .build());
                                    break;
                                case ("STRING"):
                                    queryables.addQueryables(ImmutableQueryable.builder()
                                            .id(dbg4.getName())
                                            .type("string")
                                            .build());
                                    break;
                                case ("NUMBER"):
                                case ("DOUBLE"):
                                    queryables.addQueryables(ImmutableQueryable.builder()
                                            .id(dbg4.getName())
                                            .type("number")
                                            .build());
                                    break;
                                case ("INTEGER"):
                                    queryables.addQueryables(ImmutableQueryable.builder()
                                            .id(dbg4.getName())
                                            .type("integer")
                                            .build());
                                    break;
                            }
                        }
                    }
                    else if (dbg4.isTemporal()) {
                        queryables.addQueryables(ImmutableQueryable.builder()
                                .id(dbg4.getName())
                                .type("dateTime")
                                .build());

                    }
                    else if (dbg4.isSpatial()) {
                        // do not include spatial geometry properties
                    }

                }
            }
        });

        queryables.links(links);

        Optional<OgcApiQueryablesFormatExtension> outputFormatExtension = api.getOutputFormat(
                OgcApiQueryablesFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/queryables");

        if (outputFormatExtension.isPresent()) {
            Response queryablesResponse = outputFormatExtension.get().getResponse(queryables.build(), collectionId, api, requestContext);

            Response.ResponseBuilder response = Response.ok()
                    .entity(queryablesResponse.getEntity())
                    .type(requestContext
                            .getMediaType()
                            .type());

            Optional<Locale> language = requestContext.getLanguage();
            if (language.isPresent())
                response.language(language.get());

            if (queryInput.getIncludeLinkHeader() && links != null)
                links.stream().forEach(link -> response.links(link.getLink()));

            return response.build();
        }

        throw new NotAcceptableException();
    }
}
