/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.oas30.app.ExtendableOpenApiDefinitionImpl;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetContent;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MediaType;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class StyleFormatSld10 implements ConformanceClass, StyleFormatExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(StyleFormatSld10.class);

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.ogc.sld+xml", ImmutableMap.of("version", "1.0")))
            .label("SLD 1.0")
            .parameter("sld10")
            .build();

    final private Optional<Validator> validator;

    public StyleFormatSld10() {
        Validator validator1;
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(Resources.getResource(StyleFormatSld10.class, "/schemas/sld10.xsd"));
            validator1 = schema.newValidator();
        } catch (SAXException e) {
            LOGGER.error("StyleFormatSld10 initialization failed: Could not process SLD 1.0 XSD.");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace: ", e);
            }
            validator1 = null;
        }
        this.validator = Optional.ofNullable(validator1);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/sld-10");
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "sld10";
    }

    @Override
    public String getSpecification() {
        return "https://www.ogc.org/standards/sld";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public Object getStyleEntity(StylesheetContent stylesheetContent, OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext) {
        return stylesheetContent.getContent();
    }

    @Override
    public Optional<String> analyze(StylesheetContent stylesheetContent, boolean strict) {

        if (strict && validator.isPresent()) {
            try {
                validator.get().validate(new StreamSource(ByteSource.wrap(stylesheetContent.getContent()).openStream()));
            } catch (IOException | SAXException e) {
                throw new IllegalArgumentException(String.format("The SLD 1.0 stylesheet '%s' is invalid.", stylesheetContent.getDescriptor()), e);
            }
        }

        // TODO derive name
        return Optional.empty();
    }
}
