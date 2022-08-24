/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.gltf.domain.BufferView;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiBackgroundTask;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles3d.domain.Availability;
import de.ii.ogcapi.tiles3d.domain.Format3dTilesSubtree;
import de.ii.ogcapi.tiles3d.domain.ImmutableQueryInputSubtree;
import de.ii.ogcapi.tiles3d.domain.ImmutableTileResource;
import de.ii.ogcapi.tiles3d.domain.ImmutableTileResource.Builder;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles.QueryInputSubtree;
import de.ii.ogcapi.tiles3d.domain.SeedingOptions;
import de.ii.ogcapi.tiles3d.domain.Subtree;
import de.ii.ogcapi.tiles3d.domain.TileResource;
import de.ii.ogcapi.tiles3d.domain.TileResource.TYPE;
import de.ii.ogcapi.tiles3d.domain.TileResourceCache;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.shape.fractal.MortonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for a automatic generation of the Tiles. The range is specified in the
 * config. The automatic generation is executed, when the server is started/restarted.
 */
@Singleton
@AutoBind
public class Seeding implements OgcApiBackgroundTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(Seeding.class);
  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("model", "gltf-binary"))
          .label("glTF-Binary")
          .parameter("glb")
          .build();

  private final CrsTransformerFactory crsTransformerFactory;
  private final ExtensionRegistry extensionRegistry;
  private final TileResourceCache tileResourcesCache;
  private final URI servicesUri;
  private final FeaturesCoreProviders providers;
  private final FeaturesCoreQueriesHandler queryHandlerFeatures;
  private final QueriesHandler3dTiles queryHandler;
  private final Cql cql;
  private final FeaturesQuery featuresQuery;

  @Inject
  public Seeding(
      CrsTransformerFactory crsTransformerFactory,
      ExtensionRegistry extensionRegistry,
      TileResourceCache tileResourcesCache,
      ServicesContext servicesContext,
      FeaturesCoreProviders providers,
      FeaturesCoreQueriesHandler queryHandlerFeatures,
      QueriesHandler3dTiles queryHandler,
      Cql cql,
      FeaturesQuery featuresQuery) {
    this.crsTransformerFactory = crsTransformerFactory;
    this.extensionRegistry = extensionRegistry;
    this.tileResourcesCache = tileResourcesCache;
    this.servicesUri = servicesContext.getUri();
    this.providers = providers;
    this.queryHandlerFeatures = queryHandlerFeatures;
    this.queryHandler = queryHandler;
    this.cql = cql;
    this.featuresQuery = featuresQuery;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    if (!apiData.getEnabled()) {
      return false;
    }
    // no vector tiles support for WFS backends
    if (!providers
        .getFeatureProvider(apiData)
        .map(FeatureProvider2::supportsHighLoad)
        .orElse(false)) {
      return false;
    }

    // no formats available
    if (extensionRegistry.getExtensionsForType(Format3dTilesSubtree.class).isEmpty()) {
      return false;
    }

    // TODO content format

    return apiData
        .getExtension(Tiles3dConfiguration.class)
        .filter(Tiles3dConfiguration::isEnabled)
        // TODO seeding only for features as tile providers
        .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public Class<OgcApi> getServiceType() {
    return OgcApi.class;
  }

  @Override
  public String getLabel() {
    return "3D Tiles seeding";
  }

  @Override
  public boolean runOnStart(OgcApi api) {
    return isEnabledForApi(api.getData())
        && api.getData()
            .getExtension(Tiles3dConfiguration.class)
            .flatMap(Tiles3dConfiguration::getSeedingOptions)
            .filter(seedingOptions -> !seedingOptions.shouldRunOnStartup())
            .isEmpty();
  }

  @Override
  public Optional<String> runPeriodic(OgcApi api) {
    if (!isEnabledForApi(api.getData())) {
      return Optional.empty();
    }
    return api.getData()
        .getExtension(Tiles3dConfiguration.class)
        .flatMap(Tiles3dConfiguration::getSeedingOptions)
        .flatMap(SeedingOptions::getCronExpression);
  }

  @Override
  public int getMaxPartials(OgcApi api) {
    return api.getData()
        .getExtension(Tiles3dConfiguration.class)
        .flatMap(Tiles3dConfiguration::getSeedingOptions)
        .map(SeedingOptions::getEffectiveMaxThreads)
        .orElse(1);
  }

  private boolean shouldPurge(OgcApi api) {
    return api.getData()
        .getExtension(Tiles3dConfiguration.class)
        .flatMap(Tiles3dConfiguration::getSeedingOptions)
        .filter(SeedingOptions::shouldPurge)
        .isPresent();
  }

  /**
   * Run the seeding
   *
   * @param api
   * @param taskContext
   */
  @Override
  public void run(OgcApi api, TaskContext taskContext) {
    if (shouldPurge(api) && taskContext.isFirstPartial()) {
      try {
        taskContext.setStatusMessage("purging cache");
        tileResourcesCache.deleteTiles(api);
        taskContext.setStatusMessage("purged cache successfully");
      } catch (IOException e) {
        LOGGER.debug("{}: purging failed | {}", getLabel(), e.getMessage());
      }
    }

    try {
      // add any additional single-layer tiles
      if (!taskContext.isStopped()) seed(api, taskContext);

    } catch (IOException e) {
      if (!taskContext.isStopped()) {
        throw new RuntimeException("Error accessing the tile cache during seeding.", e);
      }
    } catch (Throwable e) {
      // in general, this should only happen on shutdown (as we cannot influence shutdown order,
      // exceptions
      // during seeding on shutdown are currently inevitable), but for other situations we still add
      // the error
      // to the log
      if (!taskContext.isStopped()) {
        throw new RuntimeException(
            "An error occurred during seeding. Note that this may be a side-effect of a server shutdown.",
            e);
      }
    }
  }

  private void seed(OgcApi api, TaskContext taskContext) throws IOException {
    OgcApiDataV2 apiData = api.getData();
    // isEnabled checks that we have a feature provider
    FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(apiData);

    long numberOfTiles = getNumberOfSubtrees(api, taskContext);
    final double[] currentSubtree = {0.0};

    walkCollectionsAndTiles(
        api,
        taskContext,
        (subtree) -> {
          String collectionId = subtree.getCollectionId();
          int level = subtree.getLevel();
          int x = subtree.getX();
          int y = subtree.getY();

          /* TODO support multiple threads
          if (taskContext.isPartial() && !taskContext.matchesPartialModulo(col)) {
                continue;
          }
           */

          taskContext.setStatusMessage(
              String.format(
                  "currently processing subtree -> %s, %s/%s/%s", collectionId, level, x, y));

          byte[] subtreeBytes = null;
          try {
            if (tileResourcesCache.tileResourceExists(subtree)) {
              // already there, nothing to create, but advance progress and process content
              // availability
              subtreeBytes =
                  tileResourcesCache
                      .getTileResource(subtree)
                      .map(
                          stream -> {
                            try {
                              ByteArrayOutputStream baos = new ByteArrayOutputStream();
                              ByteStreams.copy(stream, baos);
                              return baos.toByteArray();
                            } catch (IOException e) {
                              throw new IllegalStateException(e.getMessage(), e);
                            }
                          })
                      .orElse(null);
            }
          } catch (Exception e) {
            LOGGER.warn(
                "Failed to retrieve tile {}/{}/{} for collection {} from the cache. Reason: {}",
                level,
                x,
                y,
                collectionId,
                e.getMessage());
            if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
              LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
            }
          }

          final Tiles3dConfiguration cfg =
              apiData.getExtension(Tiles3dConfiguration.class, collectionId).orElseThrow();
          final int subtreeLevels = Objects.requireNonNull(cfg.getSubtreeLevels());

          if (Objects.isNull(subtreeBytes)) {
            final QueryInputSubtree queryInput =
                ImmutableQueryInputSubtree.builder()
                    .api(api)
                    .featureProvider(providers.getFeatureProviderOrThrow(api.getData()))
                    .featureType(
                        api.getData()
                            .getExtension(FeaturesCoreConfiguration.class, collectionId)
                            .flatMap(FeaturesCoreConfiguration::getFeatureType)
                            .orElse(collectionId))
                    .geometryProperty(
                        providers
                            .getFeatureSchema(
                                api.getData(),
                                api.getData().getCollectionData(collectionId).orElseThrow())
                            .flatMap(SchemaBase::getPrimaryGeometry)
                            .map(SchemaBase::getFullPathAsString)
                            .orElseThrow())
                    .servicesUri(servicesUri)
                    .collectionId(collectionId)
                    .level(level)
                    .x(x)
                    .y(y)
                    .maxLevel(Objects.requireNonNull(cfg.getMaxLevel()))
                    .firstLevelWithContent(Objects.requireNonNull(cfg.getFirstLevelWithContent()))
                    .subtreeLevels(subtreeLevels)
                    .contentFilters(
                        cfg.getContentFilters().stream()
                            .map(filter -> cql.read(filter, Format.TEXT))
                            .collect(Collectors.toUnmodifiableList()))
                    .tileFilters(
                        cfg.getTileFilters().stream()
                            .map(filter -> cql.read(filter, Format.TEXT))
                            .collect(Collectors.toUnmodifiableList()))
                    .build();

            subtreeBytes = Tiles3dHelper.getSubtree(queryHandlerFeatures, queryInput, subtree);

            try {
              tileResourcesCache.storeTileResource(subtree, subtreeBytes);
            } catch (IOException e) {
              LOGGER.warn(
                  "{}: writing subtree failed -> {}, {}/{}/{} | {}",
                  getLabel(),
                  collectionId,
                  level,
                  x,
                  y,
                  e.getMessage());
              if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
                LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
              }
            }
          }

          final int jsonLength = Tiles3dHelper.littleEndianLongToInt(subtreeBytes, 8);
          final byte[] jsonContent = Arrays.copyOfRange(subtreeBytes, 24, 24 + jsonLength);
          final Subtree subtreeJson = Tiles3dHelper.readSubtree(jsonContent);
          final Availability contentAvailability = subtreeJson.getContentAvailability().get(0);

          byte[] buffer = null;
          boolean constant = false;
          if (contentAvailability.getBitstream().isPresent()) {
            // generate partial content
            final BufferView bv =
                contentAvailability
                    .getBitstream()
                    .map(i -> subtreeJson.getBufferViews().get(i))
                    .orElseThrow();
            final int baseOffset = 24 + jsonLength + bv.getByteOffset();
            buffer = Arrays.copyOfRange(subtreeBytes, baseOffset, baseOffset + bv.getByteLength());
          } else if (contentAvailability.getConstant().filter(c -> c == 1).isPresent()) {
            constant = true;
          }

          // generate content
          if (constant || Objects.nonNull(buffer)) {
            int xl = x;
            int yl = y;
            for (int il = 0; il < subtreeLevels; il++) {
              for (int idx = 0; idx < MortonCode.size(il); idx++) {
                if (constant || Tiles3dHelper.getAvailability(buffer, 0, il, idx)) {
                  Coordinate coord = MortonCode.decode(idx);
                  processContent(
                      api,
                      taskContext,
                      collectionId,
                      level + il,
                      xl + (int) coord.x,
                      yl + (int) coord.y);
                }
              }
              xl *= 2;
              yl *= 2;
            }
          }

          final Availability childSubtreeAvailability = subtreeJson.getChildSubtreeAvailability();
          LinkedList<TileResource> next = new LinkedList<>();
          buffer = null;
          constant = false;
          if (childSubtreeAvailability.getBitstream().isPresent()) {
            // generate partial content
            final BufferView bv =
                childSubtreeAvailability
                    .getBitstream()
                    .map(i -> subtreeJson.getBufferViews().get(i))
                    .orElseThrow();
            final int baseOffset = 24 + jsonLength + bv.getByteOffset();
            buffer = Arrays.copyOfRange(subtreeBytes, baseOffset, baseOffset + bv.getByteLength());
          } else if (childSubtreeAvailability.getConstant().filter(c -> c == 1).isPresent()) {
            constant = true;
          }

          if (constant || Objects.nonNull(buffer)) {
            int xl = x * (int) Math.pow(2, subtreeLevels);
            int yl = y * (int) Math.pow(2, subtreeLevels);
            for (int idx = 0; idx < MortonCode.size(subtreeLevels); idx++) {
              if (constant
                  || Tiles3dHelper.getAvailability(buffer, subtreeLevels, subtreeLevels, idx)) {
                Coordinate coord = MortonCode.decode(idx);
                next.add(
                    new ImmutableTileResource.Builder()
                        .api(api)
                        .collectionId(collectionId)
                        .type(TYPE.SUBTREE)
                        .level(level + subtreeLevels)
                        .x(xl + (int) coord.x)
                        .y(yl + (int) coord.y)
                        .build());
              }
            }
          }

          currentSubtree[0] += 1;
          taskContext.setCompleteness(currentSubtree[0] / numberOfTiles);

          return next;
        });
  }

  private void processContent(
      OgcApi api, TaskContext taskContext, String collectionId, int il, int ix, int iy) {
    final TileResource content =
        new Builder()
            .type(TYPE.CONTENT)
            .api(api)
            .collectionId(collectionId)
            .level(il)
            .x(ix)
            .y(iy)
            .build();
    try {
      if (!tileResourcesCache.tileResourceExists(content)) {
        taskContext.setStatusMessage(
            String.format(
                "currently processing content -> %s, %s/%s/%s", collectionId, il, ix, iy));
        // generate and store content il ix iy
        Tiles3dConfiguration cfg =
            api.getData().getExtension(Tiles3dConfiguration.class, collectionId).orElseThrow();
        Tiles3dHelper.getContent(
            featuresQuery,
            providers,
            queryHandlerFeatures,
            tileResourcesCache,
            new URICustomizer(
                String.format(
                    "%s/collections/%s/3dtiles/content/%d/%d/%d",
                    servicesUri.toString(), collectionId, il, ix, iy)),
            cfg,
            content,
            cql,
            Optional.empty(),
            MEDIA_TYPE);
      }
    } catch (Exception e) {
      LOGGER.warn(
          "{}: writing content failed -> {}, {}/{}/{} | {}",
          getLabel(),
          collectionId,
          il,
          ix,
          iy,
          e.getMessage());
      if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
        LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
      }
    }
  }

  private long getNumberOfSubtrees(OgcApi api, TaskContext taskContext) {
    final long[] numberOfSubtrees = {0};

    try {
      walkCollectionsAndTiles(
          api,
          taskContext,
          (subtree) -> {
            numberOfSubtrees[0]++;
            // TODO
            return ImmutableList.of();
          });
    } catch (IOException e) {
      // ignore
    }

    return numberOfSubtrees[0];
  }

  interface TileWalker {
    List<TileResource> visit(TileResource subtree) throws IOException;
  }

  private void walkCollectionsAndTiles(OgcApi api, TaskContext taskContext, TileWalker tileWalker)
      throws IOException {
    for (Entry<String, FeatureTypeConfigurationOgcApi> entry :
        api.getData().getCollections().entrySet()) {
      if (api.getData()
          .getExtension(Tiles3dConfiguration.class, entry.getKey())
          .map(ExtensionConfiguration::isEnabled)
          .isPresent()) {
        String collectionId = entry.getKey();
        walkTiles(api, collectionId, taskContext, tileWalker);
      }
    }
  }

  private void walkTiles(
      OgcApi api, String collectionId, TaskContext taskContext, TileWalker tileWalker)
      throws IOException {
    LinkedList<TileResource> queue = new LinkedList<>();
    queue.add(
        new ImmutableTileResource.Builder()
            .api(api)
            .collectionId(collectionId)
            .type(TYPE.SUBTREE)
            .level(0)
            .x(0)
            .y(0)
            .build());
    while (!queue.isEmpty()) {
      TileResource subtree = queue.removeFirst();
      queue.addAll(tileWalker.visit(subtree));
    }
  }
}
