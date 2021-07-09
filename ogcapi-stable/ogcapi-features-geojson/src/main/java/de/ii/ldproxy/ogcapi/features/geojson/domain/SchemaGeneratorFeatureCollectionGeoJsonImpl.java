/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeatureCollection;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class SchemaGeneratorFeatureCollectionGeoJsonImpl extends SchemaGeneratorFeatureCollection implements SchemaGeneratorFeatureCollectionGeoJson {

    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<VERSION, ConcurrentMap<SchemaGeneratorFeature.SCHEMA_TYPE, JsonSchemaObject>>>> schemaMapJson = new ConcurrentHashMap<>();

    @Override
    public JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type) {
        return getSchemaJson(apiData, collectionId, schemaUri, type, VERSION.V201909);
    }

    @Override
    public JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, Optional<VERSION> version) {
        return version.isEmpty()
            ? getSchemaJson(apiData, collectionId, schemaUri, type)
            : getSchemaJson(apiData, collectionId, schemaUri, type, version.get());
    }

    @Override
    public JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, VERSION version) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMapJson.containsKey(apiHashCode))
            schemaMapJson.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMapJson.get(apiHashCode).containsKey(collectionId))
            schemaMapJson.get(apiHashCode).put(collectionId, new ConcurrentHashMap<>());
        if (!schemaMapJson.get(apiHashCode).get(collectionId).containsKey(version))
            schemaMapJson.get(apiHashCode).get(collectionId).put(version, new ConcurrentHashMap<>());
        if (!schemaMapJson.get(apiHashCode).get(collectionId).get(version).containsKey(type)) {

            ImmutableJsonSchemaObject.Builder schemaBuilder = ImmutableJsonSchemaObject.builder()
                                                                                       .schema(getSchemaUri(version))
                                                                                       .id(schemaUri)
                                                                                       .required(ImmutableList.of("type", "features"))
                                                                                       .putProperties("type", ImmutableJsonSchemaString.builder()
                                                                                                                                       .addEnums("FeatureCollection")
                                                                                                                                       .build())
                                                                                       .putProperties("features", ImmutableJsonSchemaArray.builder()
                                                                                                                                          .items(ImmutableJsonSchemaRef.builder()
                                                                                                                                                                       .ref(schemaUri.map(uri -> uri.replace("/schemas/collection", "/schemas/feature"))
                                                                                                                                                                                     .orElse("https://geojson.org/schema/Feature.json"))
                                                                                                                                                                       .build())
                                                                                                                                          .build())
                                                                                       .putProperties("links", ImmutableJsonSchemaArray.builder()
                                                                                                                                       .items(ImmutableJsonSchemaRef.builder()
                                                                                                                                                                    .ref("#/" + getDefinitionsToken(version) + "/Link")
                                                                                                                                                                    .build())
                                                                                                                                       .build())
                                                                                       .putProperties("timeStamp", ImmutableJsonSchemaString.builder()
                                                                                                                                            .format("date-time")
                                                                                                                                            .build())
                                                                                       .putProperties("numberMatched", ImmutableJsonSchemaInteger.builder()
                                                                                                                                                 .minimum(0)
                                                                                                                                                 .build())
                                                                                       .putProperties("numberReturned", ImmutableJsonSchemaInteger.builder()
                                                                                                                                                  .minimum(0)
                                                                                                                                                  .build());
            switch (getDefinitionsToken(version)) {
                case "$defs":
                    schemaBuilder.defs(ImmutableMap.of("Link", LINK_JSON));
                    break;
                case "definitions":
                    schemaBuilder.definitions(ImmutableMap.of("Link", LINK_JSON));
                    break;
            }

            schemaMapJson.get(apiHashCode)
                         .get(collectionId)
                         .get(version)
                         .put(type, schemaBuilder.build());
        }
        return schemaMapJson.get(apiHashCode).get(collectionId).get(version).get(type);
    }
}
