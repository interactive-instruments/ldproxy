/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ogcapi.styles.domain.StylesheetContent;
import de.ii.ogcapi.styles.domain.Tiles3dStylesheet;
import de.ii.xtraplatform.base.domain.LogContext;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title 3D Tiles Declarative Styling
 */
@Singleton
@AutoBind
public class StyleFormat3dTiles implements StyleFormatExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormat3dTiles.class);

  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("3D Tiles")
          .parameter("3dtiles")
          .fileExtension("json")
          .build();

  private final Schema<?> schemaStyle;
  private final Map<String, Schema<?>> referencedSchemas;

  @Inject
  public StyleFormat3dTiles(ClassSchemaCache classSchemaCache) {
    this.schemaStyle = classSchemaCache.getSchema(Tiles3dStylesheet.class);
    referencedSchemas = classSchemaCache.getReferencedSchemas(Tiles3dStylesheet.class);
  }

  @Override
  public boolean canSupportTransactions() {
    return true;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schemaStyle)
        .schemaRef(Tiles3dStylesheet.SCHEMA_REF)
        .referencedSchemas(referencedSchemas)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public String getFileExtension() {
    return "3dtiles";
  }

  @Override
  public String getSpecification() {
    return "https://github.com/CesiumGS/3d-tiles/tree/main/specification/Styling";
  }

  @Override
  public String getVersion() {
    return "1.1";
  }

  @Override
  public boolean getAsDefault() {
    return true;
  }

  @Override
  public String getTitle(String styleId, StylesheetContent stylesheetContent) {
    return parse(stylesheetContent, false, false)
        .map(Tiles3dStylesheet::getExtras)
        .map(e -> e.containsKey("name") ? e.get("name").toString() : styleId)
        .orElse(styleId);
  }

  @Override
  public Object getStyleEntity(
      StylesheetContent stylesheetContent,
      OgcApi api,
      Optional<String> collectionId,
      String styleId,
      ApiRequestContext requestContext) {
    return parse(stylesheetContent, true, false);
  }

  @Override
  public boolean canDeriveCollectionStyle() {
    return false;
  }

  @Override
  public boolean canDeriveMetadata() {
    return false;
  }

  @Override
  public Optional<String> analyze(StylesheetContent stylesheetContent, boolean strict) {
    Tiles3dStylesheet stylesheet = parse(stylesheetContent, true, strict).get();

    // TODO add checks

    Optional<String> styleIdCandidate =
        stylesheet.getExtras().containsKey("name")
            ? Optional.of(stylesheet.getExtras().get("name").toString())
            : Optional.empty();
    if (styleIdCandidate.isPresent()) {
      Pattern styleNamePattern = Pattern.compile("[^\\w\\-]", Pattern.CASE_INSENSITIVE);
      Matcher styleNameMatcher = styleNamePattern.matcher(styleIdCandidate.get());
      if (styleIdCandidate.get().contains(" ") || styleNameMatcher.find()) return Optional.empty();
    }

    return styleIdCandidate;
  }

  static Optional<Tiles3dStylesheet> parse(
      StylesheetContent stylesheetContent, boolean throwOnError, boolean strict) {
    final byte[] content = stylesheetContent.getContent();

    // prepare Jackson mapper for deserialization
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, strict);
    mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    Tiles3dStylesheet parsedContent;
    try {
      // parse input
      parsedContent = mapper.readValue(content, Tiles3dStylesheet.class);
    } catch (IOException e) {
      if (stylesheetContent.getInStore()) {
        // this is an invalid style already in the store: server error
        if (throwOnError)
          throw new RuntimeException(
              "The content of a stylesheet is invalid: " + stylesheetContent.getDescriptor() + ".",
              e);
      } else {
        // a style provided by a client: client error
        if (throwOnError)
          throw new IllegalArgumentException("The content of the stylesheet is invalid.", e);
      }
      LOGGER.error(
          "The content of a stylesheet ''{}'' is invalid: {}",
          stylesheetContent.getDescriptor(),
          e.getMessage());
      if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
        LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
      }
      return Optional.empty();
    }

    return Optional.of(parsedContent);
  }
}
