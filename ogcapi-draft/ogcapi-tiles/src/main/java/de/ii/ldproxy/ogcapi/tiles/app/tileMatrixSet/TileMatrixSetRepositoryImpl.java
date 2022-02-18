/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.foundation.domain.FoundationConfiguration.CACHE_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * Access to the cache for tile files.
 */
@Singleton
@AutoBind
public class TileMatrixSetRepositoryImpl implements TileMatrixSetRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileMatrixSetRepositoryImpl.class);
    private static final String API_RESOURCES_DIR_NAME = "api-resources";
    private static final String TILE_MATRIX_SET_DIR_NAME = "tile-matrix-sets";
    private final Path customTileMatrixSetsStore;
    private final Map<String, TileMatrixSet> tileMatrixSets;

    /**
     * set data directory
     */
    public TileMatrixSetRepositoryImpl(@Context BundleContext bundleContext) {
        this.customTileMatrixSetsStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR_NAME, TILE_MATRIX_SET_DIR_NAME);
        this.tileMatrixSets = new HashMap<>();
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                                                                             .mode(apiValidation);

        if (tileMatrixSets.isEmpty())
            initCache();

        return builder.build();
    }

    @Override
    public Optional<TileMatrixSet> get(String tileMatrixSetId) {
        if (tileMatrixSets.isEmpty())
            initCache();
        return Optional.ofNullable(tileMatrixSets.get(tileMatrixSetId));
    }

    @Override
    public Map<String, TileMatrixSet> getAll() {
        if (tileMatrixSets.isEmpty())
            initCache();
        return new ImmutableMap.Builder<String, TileMatrixSet>().putAll(tileMatrixSets).build();
    }

    private void initCache() {
        ImmutableList.of("WebMercatorQuad", "WorldCRS84Quad", "WorldMercatorWGS84Quad", "AdV_25832", "EU_25832", "gdi_de_25832")
                     .forEach(tileMatrixSetId -> TileMatrixSet.fromWellKnownId(tileMatrixSetId).ifPresent(tms -> tileMatrixSets.put(tileMatrixSetId, tms)));

        Arrays.stream(Objects.requireNonNullElse(customTileMatrixSetsStore.toFile().listFiles(), ImmutableList.of().toArray(File[]::new)))
              .filter(file -> !file.isHidden())
              .filter(file -> com.google.common.io.Files.getFileExtension(file.getName()).equals("json"))
              .map(file -> com.google.common.io.Files.getNameWithoutExtension(file.getName()))
              .forEach(tileMatrixSetId -> TileMatrixSet.fromDefinition(tileMatrixSetId, customTileMatrixSetsStore).ifPresent(tms -> tileMatrixSets.put(tileMatrixSetId, tms)));
    }

}
