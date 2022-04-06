/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.collections.app.JacksonSubTypeIdsOgcApiCollections;
import de.ii.ogcapi.collections.queryables.app.JacksonSubTypeIdsQueryables;
import de.ii.ogcapi.collections.schema.app.JacksonSubTypeIdsSchema;
import de.ii.ogcapi.common.domain.JacksonSubTypeIdsOgcApiCommon;
import de.ii.ogcapi.crs.app.JacksonSubTypeIdsCrs;
import de.ii.ogcapi.features.core.app.JacksonSubTypeIdsFeaturesCore;
import de.ii.ogcapi.features.custom.extensions.app.JacksonSubTypeIdsFeaturesExtensions;
import de.ii.ogcapi.features.geojson.app.JacksonSubTypeIdsGeoJson;
import de.ii.ogcapi.features.geojson.ld.app.JacksonSubTypeIdsGeoJsonLd;
import de.ii.ogcapi.features.gml.app.JacksonSubTypeIdsGml;
import de.ii.ogcapi.features.html.app.JacksonSubTypeIdsFeaturesHtml;
import de.ii.ogcapi.features.json.fg.app.JacksonSubTypeIdsJsonFg;
import de.ii.ogcapi.filter.domain.JacksonSubTypeIdsFilter;
import de.ii.ogcapi.foundation.app.OgcApiFactory;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.JacksonSubTypeIdsFoundation;
import de.ii.ogcapi.geometry.simplification.app.JacksonSubTypeIdsGeometrySimplification;
import de.ii.ogcapi.html.app.JacksonSubTypeIdsHtml;
import de.ii.ogcapi.json.app.JacksonSubTypeIdsJson;
import de.ii.ogcapi.maps.app.JacksonSubTypeIdsMapTiles;
import de.ii.ogcapi.oas30.app.JacksonSubTypeIdsOas30;
import de.ii.ogcapi.projections.app.JacksonSubTypeIdsProjections;
import de.ii.ogcapi.resources.app.JacksonSubTypeIdsResources;
import de.ii.ogcapi.sorting.app.JacksonSubTypeIdsSorting;
import de.ii.ogcapi.styles.app.JacksonSubTypeIdsStyles;
import de.ii.ogcapi.tiles.app.JacksonSubTypeIdsTiles;
import de.ii.ogcapi.transactional.app.JacksonSubTypeIdsTransactional;
import de.ii.ogcapi.xml.app.JacksonSubTypeIdsXml;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.codelists.app.CodelistEntity;
import de.ii.xtraplatform.codelists.app.CodelistFactory;
import de.ii.xtraplatform.codelists.app.CodelistFactory.CodelistFactoryAssisted;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.feature.provider.pgis.FeatureProviderRegisterPgis;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSqlFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface EntityFactories {

  static Set<EntityFactory> factories(ExtensionRegistry extensionRegistry) {
    return ImmutableSet.<EntityFactory>builder()
        .add(new CodelistFactory(null) {
          @Override
          public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public void deleteInstance(String id) {

          }

          @Override
          public void addEntityListener(Consumer<PersistentEntity> listener, boolean existing) {

          }

          @Override
          public void addEntityGoneListener(Consumer<PersistentEntity> listener) {

          }
        })
        .add(new FeatureProviderSqlFactory(null, null, null, null, null, null, null, null) {
          @Override
          public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public void deleteInstance(String id) {

          }

          @Override
          public void addEntityListener(Consumer<PersistentEntity> listener, boolean existing) {

          }

          @Override
          public void addEntityGoneListener(Consumer<PersistentEntity> listener) {

          }
        })
        .add(new OgcApiFactory(null, extensionRegistry, null) {
          @Override
          public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public void deleteInstance(String id) {

          }

          @Override
          public void addEntityListener(Consumer<PersistentEntity> listener, boolean existing) {

          }

          @Override
          public void addEntityGoneListener(Consumer<PersistentEntity> listener) {

          }
        })
        .build();
  }
}
