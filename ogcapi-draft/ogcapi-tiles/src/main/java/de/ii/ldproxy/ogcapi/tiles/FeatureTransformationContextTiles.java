/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.codelists.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import org.immutables.value.Value;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
public abstract class FeatureTransformationContextTiles implements FeatureTransformationContext {

    public abstract Map<String, Object> getProcessingParameters();

    public abstract Tile getTile();
    public abstract File getTileFile();
    public abstract CrsTransformerFactory getCrsTransformerFactory();

    @Value.Derived
    public TilesConfiguration getConfiguration() {
        TilesConfiguration configuration = null;

        Optional<TilesConfiguration> baseConfiguration = getApiData().getExtension(TilesConfiguration.class);

        Optional<TilesConfiguration> collectionConfiguration = Optional.ofNullable(getApiData().getCollections()
                .get(getCollectionId()))
                .flatMap(featureTypeConfiguration -> featureTypeConfiguration.getExtension(TilesConfiguration.class));

        if (collectionConfiguration.isPresent()) {
            configuration = collectionConfiguration.get();
        }

        if (baseConfiguration.isPresent()) {
            if (Objects.isNull(configuration)) {
                configuration = baseConfiguration.get();
            } else {
                configuration = configuration.mergeDefaults(baseConfiguration.get());
            }
        }

        return configuration;
    }

}
