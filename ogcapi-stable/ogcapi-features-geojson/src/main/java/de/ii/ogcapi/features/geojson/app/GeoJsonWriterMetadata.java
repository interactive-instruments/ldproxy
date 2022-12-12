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
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalLong;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class GeoJsonWriterMetadata implements GeoJsonWriter {

  @Inject
  public GeoJsonWriterMetadata() {}

  @Override
  public GeoJsonWriterMetadata create() {
    return new GeoJsonWriterMetadata();
  }

  @Override
  public int getSortPriority() {
    return 20;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (context.encoding().isFeatureCollection()) {
      OptionalLong numberReturned = context.metadata().getNumberReturned();
      OptionalLong numberMatched = context.metadata().getNumberMatched();

      if (numberReturned.isPresent()) {
        context.encoding().getJson().writeNumberField("numberReturned", numberReturned.getAsLong());
      }
      if (numberMatched.isPresent()) {
        context.encoding().getJson().writeNumberField("numberMatched", numberMatched.getAsLong());
      }
      context
          .encoding()
          .getJson()
          .writeStringField("timeStamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }

    next.accept(context);
  }
}
