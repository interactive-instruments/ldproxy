/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.tiles.domain.WmtsCapabilitiesFormatExtension;
import de.ii.ogcapi.tiles.domain.WmtsServiceMetadata;
import de.ii.xtraplatform.tiles.domain.JacksonXmlAnnotation.XmlIgnore;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @title WMTS 1.0.0 Capabilities
 */
@Singleton
@AutoBind
public class WmtsCapabilitiesXml implements WmtsCapabilitiesFormatExtension {

  public class XmlAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {
    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
      return m.hasAnnotation(XmlIgnore.class) || super.hasIgnoreMarker(m);
    }
  }

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "xml"))
          .label("XML")
          .parameter("xml")
          .build();

  @Inject
  WmtsCapabilitiesXml() {
    super();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(OBJECT_SCHEMA)
        .schemaRef(OBJECT_SCHEMA_REF)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public Object getEntity(
      WmtsServiceMetadata capabilities, OgcApi api, ApiRequestContext requestContext) {
    try {
      XmlMapper mapper = new XmlMapper();
      mapper
          .registerModule(new Jdk8Module())
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
          .setAnnotationIntrospector(new XmlAnnotationIntrospector());

      return mapper.writeValueAsString(capabilities);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
