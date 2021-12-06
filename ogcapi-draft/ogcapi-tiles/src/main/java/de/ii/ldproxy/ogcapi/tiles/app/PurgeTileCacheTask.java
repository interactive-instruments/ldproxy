/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author zahnen
 */
@Component
@Instantiate
public class PurgeTileCacheTask extends Task {

  private static final Logger LOGGER = LoggerFactory.getLogger(PurgeTileCacheTask.class);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final EntityRegistry entityRegistry;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final TileCache tileCache;

  protected PurgeTileCacheTask(@Requires Dropwizard dropwizard,
      @Requires EntityRegistry entityRegistry,
      @Requires TileMatrixSetRepository tileMatrixSetRepository, @Requires TileCache tileCache) {
    super("purge-tile-cache");
    this.entityRegistry = entityRegistry;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.tileCache = tileCache;

    dropwizard.getEnvironment().admin().addTask(this);
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Purge tile cache request: {}", parameters);
    }

    Optional<String> apiId = getId(parameters);

    if (apiId.isEmpty()) {
      output.println("No api id given");
      output.flush();
      return;
    }

    Optional<OgcApi> ogcApi = entityRegistry.getEntity(OgcApi.class, apiId.get());

    if (ogcApi.isEmpty()) {
      output.println("No api with the given id found");
      output.flush();
      return;
    }

    Optional<String> collectionId = getCollectionId(parameters);

    if (collectionId.isPresent() && !ogcApi.get().getData()
        .isCollectionEnabled(collectionId.get())) {
      output.println("No collection with the given id found");
      output.flush();
      return;
    }

    Optional<String> tileMatrixSetId = getTileMatrixSetId(parameters);

    if (tileMatrixSetId.isPresent() && tileMatrixSetRepository.get(tileMatrixSetId.get())
        .isEmpty()) {
      output.println("No tile matrix set with the given id found");
      output.flush();
      return;
    }

    List<String> bbox = getBBox(parameters);

    if (bbox.size() > 0 && bbox.size() != 4) {
      output.println("Invalid bbox given");
      output.flush();
      return;
    }

    Optional<BoundingBox> boundingBox = bbox.isEmpty()
        ? Optional.empty()
        : Optional.of(BoundingBox.of(Double.parseDouble(bbox.get(0)),
            Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)),
            Double.parseDouble(bbox.get(3)),
            OgcCrs.CRS84));

    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, apiId.get())) {
      tileCache.deleteTiles(ogcApi.get().getData(), collectionId, tileMatrixSetId, boundingBox);
    }
  }

  private Optional<String> getId(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("api")).findFirst();
  }

  private Optional<String> getCollectionId(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("collection")).findFirst();
  }

  private Optional<String> getTileMatrixSetId(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("tileMatrixSet")).findFirst();
  }

  private List<String> getBBox(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("bbox")).collect(Collectors.toList());
  }

  private Stream<String> getValueList(Collection<String> values) {
    if (Objects.isNull(values)) {
      return Stream.empty();
    }
    return values.stream()
        .flatMap(
            value -> {
              if (value.contains(",")) {
                return SPLITTER.splitToList(value).stream();
              }
              return Stream.of(value);
            });
  }
}
