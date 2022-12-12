/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.crs.domain.CoordinateTuple;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.json.domain.GeoJsonGeometryType;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
@SuppressWarnings("PMD.TooManyMethods")
public class GeoJsonWriterGeometry implements GeoJsonWriter {

  private boolean suppressPrimaryGeometry;
  private boolean geometryOpen;
  private boolean hasPrimaryGeometry;
  private boolean inPrimaryGeometry;
  private final List<String> pos = new ArrayList<>();
  // move coordinate conversion to WGS 84 to the transformation pipeline,
  // see https://github.com/interactive-instruments/ldproxy/issues/521
  private Optional<CrsTransformer> crsTransformerGeometry;

  private final CrsTransformerFactory crsTransformerFactory;

  @Inject
  public GeoJsonWriterGeometry(CrsTransformerFactory crsTransformerFactory) {
    this.crsTransformerFactory = crsTransformerFactory;
  }

  @Override
  public GeoJsonWriterGeometry create() {
    return new GeoJsonWriterGeometry(crsTransformerFactory);
  }

  @Override
  public int getSortPriority() {
    return 30;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    suppressPrimaryGeometry = context.encoding().getSuppressPrimaryGeometry();
    crsTransformerGeometry = Optional.empty();
    if (!suppressPrimaryGeometry && context.encoding().getForceDefaultCrs()) {
      EpsgCrs sourceCrs = context.encoding().getTargetCrs();
      EpsgCrs targetCrs = context.encoding().getDefaultCrs();
      if (!Objects.equals(sourceCrs, targetCrs)) {
        crsTransformerGeometry = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
      }
    }

    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    hasPrimaryGeometry = false;
    inPrimaryGeometry = false;

    next.accept(context);
  }

  @Override
  public void onObjectStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent()
        && context.geometryType().isPresent()) {
      if (suppressPrimaryGeometry
          && context.schema().map(SchemaBase::isPrimaryGeometry).orElse(false)) {
        inPrimaryGeometry = true;
      } else {
        if (context.schema().map(SchemaBase::isPrimaryGeometry).orElse(false)) {
          hasPrimaryGeometry = true;
          inPrimaryGeometry = true;
          context.encoding().stopBuffering();

          context.encoding().getJson().writeFieldName("geometry");
        } else {
          context
              .encoding()
              .getJson()
              .writeFieldName(context.schema().map(FeatureSchema::getName).orElseThrow());
        }

        context.encoding().getJson().writeStartObject();
        context
            .encoding()
            .getJson()
            .writeStringField(
                "type",
                GeoJsonGeometryType.forSimpleFeatureType(context.geometryType().get()).toString());
        context.encoding().getJson().writeFieldName("coordinates");

        this.geometryOpen = true;
      }
    }

    next.accept(context);
  }

  @Override
  public void onArrayStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (geometryOpen) {
      context.encoding().getJson().writeStartArray();
    } else if (!suppressPrimaryGeometry || !inPrimaryGeometry) {
      startBufferingIfNecessary(context);
    }

    next.accept(context);
  }

  @Override
  public void onArrayEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (geometryOpen) {
      if (!pos.isEmpty()) {
        writeCoordinates(context);
        pos.clear();
      }

      context.encoding().getJson().writeEndArray();
    }

    next.accept(context);
  }

  private void writeCoordinates(EncodingAwareContextGeoJson context) throws IOException {
    if (crsTransformerGeometry.isEmpty()) {
      // fallback
      for (String p : pos) {
        context.encoding().getJson().writeRawValue(p);
      }
    } else {
      CoordinateTuple coord =
          crsTransformerGeometry.get().transform(new CoordinateTuple(pos.get(0), pos.get(1)));
      context
          .encoding()
          .getJson()
          .writeRawValue(
              BigDecimal.valueOf(coord.getX()).setScale(7, RoundingMode.HALF_DOWN).toPlainString());
      context
          .encoding()
          .getJson()
          .writeRawValue(
              BigDecimal.valueOf(coord.getY()).setScale(7, RoundingMode.HALF_DOWN).toPlainString());
      if (pos.size() == 3) {
        context.encoding().getJson().writeRawValue(pos.get(2));
      }
    }
  }

  @Override
  public void onObjectEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent() && geometryOpen) {

      boolean stopBuffering = context.schema().map(SchemaBase::isPrimaryGeometry).orElse(false);
      if (stopBuffering) {
        context.encoding().stopBuffering();
      }

      geometryOpen = false;
      inPrimaryGeometry = false;

      // close geometry object
      context.encoding().getJson().writeEndObject();

      if (stopBuffering) {
        context.encoding().flushBuffer();
      }
    }

    next.accept(context);
  }

  @Override
  public void onValue(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (geometryOpen) {
      if (inPrimaryGeometry && context.encoding().getForceDefaultCrs()) {
        // we buffer the whole coordinate in case we force WGS84 as the CRS in "geometry"
        pos.add(context.value());
      } else {
        context.encoding().getJson().writeRawValue(context.value());
      }
    } else if (!suppressPrimaryGeometry || !inPrimaryGeometry) {
      startBufferingIfNecessary(context);
    }

    next.accept(context);
  }

  @Override
  public void onFeatureEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    // write null geometry if none was written for this feature
    if (!hasPrimaryGeometry) {
      context.encoding().stopBuffering();

      // null geometry
      context.encoding().getJson().writeFieldName("geometry");
      context.encoding().getJson().writeNull();

      context.encoding().flushBuffer();
    }

    next.accept(context);
  }

  private void startBufferingIfNecessary(EncodingAwareContextGeoJson context) {
    if (!geometryOpen && !hasPrimaryGeometry && !context.encoding().getState().isBuffering()) {
      // buffer properties until primary geometry arrives
      try {
        context.encoding().startBuffering();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
