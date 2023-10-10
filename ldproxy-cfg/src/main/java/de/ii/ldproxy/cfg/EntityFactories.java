/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.app.OgcApiFactory;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.xtraplatform.auth.app.UserFactory;
import de.ii.xtraplatform.codelists.app.CodelistFactory;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import de.ii.xtraplatform.features.gml.app.FeatureProviderWfsFactory;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSqlFactory;
import de.ii.xtraplatform.tiles.app.TileProviderFeaturesFactory;
import de.ii.xtraplatform.tiles.app.TileProviderHttpFactory;
import de.ii.xtraplatform.tiles.app.TileProviderMbTilesFactory;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface EntityFactories {

  static Set<EntityFactory> factories(ExtensionRegistry extensionRegistry) {
    return ImmutableSet.<EntityFactory>builder()
        .add(
            new CodelistFactory(null) {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new FeatureProviderSqlFactory() {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new FeatureProviderWfsFactory() {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new TileProviderFeaturesFactory() {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new TileProviderHttpFactory() {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new TileProviderMbTilesFactory() {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new OgcApiFactory(extensionRegistry) {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new UserFactory(null) {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .add(
            new UserFactory(null) {
              @Override
              public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public void deleteInstance(String id) {}

              @Override
              public void addEntityListener(
                  Consumer<PersistentEntity> listener, boolean existing) {}

              @Override
              public void addEntityGoneListener(Consumer<PersistentEntity> listener) {}
            })
        .build();
  }
}
