/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaCache;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.styles.app.SchemaCacheStyleLayer;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleStylesheet.class)
public abstract class MbStyleStylesheet {

    public enum Visibility { visible, none }

    public abstract int getVersion();
    public abstract Optional<String> getName();
    public abstract Optional<Object> getMetadata();
    public abstract Optional<List<Double>> getCenter();
    public abstract Optional<Double> getZoom();
    @Value.Default
    public Double getBearing() { return 0.0; }
    @Value.Default
    public Double getPitch() { return 0.0; }
    public abstract Optional<MbStyleLight> getLight();
    public abstract Map<String, MbStyleSource> getSources();
    public abstract Optional<String> getSprite();
    public abstract Optional<String> getGlyphs();
    public abstract Optional<MbStyleTransition> getTransition();
    public abstract List<MbStyleLayer> getLayers();

    //TODO: replace with SchemaDeriverStyleLayer
    @JsonIgnore
    public List<StyleLayer> getLayerMetadata(OgcApiDataV2 apiData, FeaturesCoreProviders providers, EntityRegistry entityRegistry) {
        // prepare a map with the JSON schemas of the feature collections used in the style
      JsonSchemaCache schemas = new SchemaCacheStyleLayer(() -> entityRegistry.getEntitiesForType(
          Codelist.class));

        Map<String, JsonSchemaObject> schemaMap = getLayers().stream()
                                                             .filter(layer -> layer.getSource().isPresent() && layer.getSource().get().equals(apiData.getId()))
                                                             .map(layer -> layer.getSourceLayer())
                                                             .filter(Optional::isPresent)
                                                             .map(Optional::get)
                                                             .distinct()
                                                             .filter(sourceLayer -> apiData.getCollections().containsKey(sourceLayer))
                                                             .map(collectionId -> new AbstractMap.SimpleImmutableEntry<>(collectionId, schemas.getSchema(
                                                                 providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId)), apiData, apiData.getCollections().get(collectionId), Optional.empty())))
                                                             .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        return getLayers().stream()
                          .map(layer -> {
                              ImmutableStyleLayer.Builder builder = ImmutableStyleLayer.builder()
                                                                                       .id(layer.getId());

                              Map<String, Object> md = layer.getMetadata();
                              if (md.containsKey("description"))
                                  builder.description(md.get("description").toString());

                              Optional<String> apiId = layer.getSource();
                              Optional<String> collectionId = layer.getSourceLayer();

                              final boolean knownSource = apiId.isPresent() && apiId.get().equals(apiData.getId()) &&
                                      collectionId.isPresent() && schemaMap.containsKey(collectionId.get());
                              final JsonSchema geometry = knownSource
                                      ? schemaMap.get(collectionId.get()).getProperties().get("geometry")
                                      : null;
                              final JsonSchemaObject properties = knownSource
                                      ? (JsonSchemaObject) schemaMap.get(collectionId.get()).getProperties().get("properties")
                                      : null;

                              ImmutableSet.Builder<String> attNamesBuilder = ImmutableSet.builder();
                              attNamesBuilder.addAll(getAttributes(layer.getFilter()));
                              layer.getLayout().values().forEach(value -> attNamesBuilder.addAll(getAttributes(value)));
                              layer.getPaint().values().forEach(value -> attNamesBuilder.addAll(getAttributes(value)));
                              Set<String> attNames = attNamesBuilder.build();

                              builder.attributes(attNames.stream()
                                                         .sorted()
                                                         .map(attName -> {
                                                             if (Objects.nonNull(properties)) {
                                                                 if (properties.getProperties().containsKey(attName))
                                                                     return new AbstractMap.SimpleImmutableEntry<>(attName, properties.getProperties().get(attName));
                                                                 return properties.getPatternProperties()
                                                                                  .entrySet()
                                                                                  .stream()
                                                                                  .filter(entry -> attName.matches(entry.getKey()))
                                                                                  .map(entry -> new AbstractMap.SimpleImmutableEntry<>(attName, entry.getValue()))
                                                                                  .findAny()
                                                                                  .orElse(null);
                                                             }
                                                             return null;
                                                         })
                                                         .filter(Objects::nonNull)
                                                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));

                              if (Objects.nonNull(geometry)) {
                                  String geomAsString = geometry.toString();
                                  boolean point = geomAsString.contains("GeoJSON Point") || geomAsString.contains("GeoJSON MultiPoint");
                                  boolean line = geomAsString.contains("GeoJSON LineString") || geomAsString.contains("GeoJSON MultiLineString");
                                  boolean polygon = geomAsString.contains("GeoJSON Polygon") || geomAsString.contains("GeoJSON MultiPolygon");
                                  if (point && !line && !polygon)
                                      builder.type("point");
                                  else if (!point && line && !polygon)
                                      builder.type("line");
                                  else if (!point && !line && polygon)
                                      builder.type("polygon");
                                  else
                                      builder.type("geometry");
                              } else if (layer.getType().matches("fill|line|symbol|fill\\-extrusion")) {
                                  builder.type("geometry");
                              } else if (layer.getType().equals("circle")) {
                                  builder.type("point");
                              } else if (layer.getType().equals("raster")) {
                                  builder.type("raster");
                              }

                              if (knownSource) {
                                  builder.sampleData(new ImmutableLink.Builder()
                                                             .rel("start")
                                                             .title("")
                                                             .href(String.format("{serviceUrl}/collections/%s/items", collectionId.get()))
                                                             .templated(true)
                                                             .build());
                              }

                              return builder.build();
                          })
                          .collect(Collectors.toUnmodifiableList());
    }

    @JsonIgnore
    private Set<String> getAttributes(Object expression) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        if (expression instanceof Optional) {
            if (((Optional) expression).isEmpty())
                return ImmutableSet.of();
            expression = ((Optional) expression).get();
        }
        if (expression instanceof Iterable) {
            Iterator it = ((Iterable) expression).iterator();
            if (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof String && ImmutableList.of("get", "has", "in").contains((String) obj) && it.hasNext()) {
                    obj = it.next();
                    if (obj instanceof String) {
                        builder.add((String) obj);
                    }
                } else {
                    while (it.hasNext()) {
                        builder.addAll(getAttributes(it.next()));
                    }
                }
            }
        }
        return builder.build();
    }

    @JsonIgnore
    public MbStyleStylesheet replaceParameters(String serviceUrl) {
        // any template parameters in links?
        boolean templated = this.getSprite()
                                .orElse("")
                                .matches("^.*\\{serviceUrl\\}.*$") ||
                this.getGlyphs()
                    .orElse("")
                    .matches("^.*\\{serviceUrl\\}.*$") ||
                this.getSources()
                    .values()
                    .stream()
                    .filter(source -> source instanceof MbStyleVectorSource || source instanceof MbStyleGeojsonSource)
                    .anyMatch(source ->
                                      (source instanceof MbStyleVectorSource &&
                                              (((MbStyleVectorSource) source).getTiles()
                                                                             .orElse(ImmutableList.of())
                                                                             .stream()
                                                                             .anyMatch(tilesUri -> tilesUri.matches("^.*\\{serviceUrl\\}.*$")) ||
                                                      ((MbStyleVectorSource) source).getUrl()
                                                                                    .orElse("")
                                                                                    .matches("^.*\\{serviceUrl\\}.*$"))) ||
                                              (source instanceof MbStyleGeojsonSource &&
                                                      (((MbStyleGeojsonSource) source).getData()
                                                                                      .filter(data -> data instanceof String)
                                                                                      .map(data -> (String) data)
                                                                                      .orElse("")
                                                                                      .matches("^.*\\{serviceUrl\\}.*$"))));
        if (!templated)
            return this;

        return ImmutableMbStyleStylesheet.builder()
                                         .from(this)
                                         .sprite(this.getSprite().isPresent() ?
                                                         Optional.of(this.getSprite().get().replace("{serviceUrl}", serviceUrl)) :
                                                         Optional.empty())
                                         .glyphs(this.getGlyphs().isPresent() ?
                                                         Optional.of(this.getGlyphs().get().replace("{serviceUrl}", serviceUrl)) :
                                                         Optional.empty())
                                         .sources(this.getSources()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(Collectors.toMap(entry -> entry.getKey(),
                                                                                entry -> {
                                                                                    MbStyleSource source = entry.getValue();
                                                                                    if (source instanceof MbStyleVectorSource)
                                                                                        return ImmutableMbStyleVectorSource.builder()
                                                                                                                           .from((MbStyleVectorSource)source)
                                                                                                                           .url(((MbStyleVectorSource)source).getUrl()
                                                                                                                                                             .map(url -> url.replace("{serviceUrl}", serviceUrl)))
                                                                                                                           .tiles(((MbStyleVectorSource)source).getTiles()
                                                                                                                                                               .orElse(ImmutableList.of())
                                                                                                                                                               .stream()
                                                                                                                                                               .map(tile -> tile.replace("{serviceUrl}", serviceUrl))
                                                                                                                                                               .collect(Collectors.toList()))
                                                                                                                           .build();
                                                                                    else if (source instanceof MbStyleGeojsonSource &&
                                                                                            ((MbStyleGeojsonSource) source).getData().isPresent() &&
                                                                                            ((MbStyleGeojsonSource) source).getData().get() instanceof String) {
                                                                                        return ImmutableMbStyleGeojsonSource.builder()
                                                                                                                     .from((MbStyleGeojsonSource)source)
                                                                                                                     .data(((MbStyleGeojsonSource)source).getData()
                                                                                                                                                         .map(data -> (String) data)
                                                                                                                                                         .map(data -> data.replace("{serviceUrl}", serviceUrl)))
                                                                                                                     .build();

                                                                                    }

                                                                                    return source;
                                                                                })))
                                         .build();
    }
}
