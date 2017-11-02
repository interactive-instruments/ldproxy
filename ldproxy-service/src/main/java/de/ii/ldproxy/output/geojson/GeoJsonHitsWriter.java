/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.gml2json.AbstractFeatureWriter;
import de.ii.ldproxy.gml2json.CoordinatesWriterType;
import de.ii.ldproxy.gml2json.JsonCoordinateFormatter;
import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fischer
 */
// TODO: implement AbstractHitsWriter
public class GeoJsonHitsWriter extends AbstractFeatureWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GeoJsonHitsWriter.class);

    public GeoJsonHitsWriter(JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection, CrsTransformer crsTransformer) {
        super(jsonOut, jsonMapper, isFeatureCollection, crsTransformer, new GeoJsonOnTheFlyMapping());
    }

    @Override
    protected void writeStart(SMInputCursor cursor) throws IOException {

        try {
            int numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));

            json.writeStartObject();
            json.writeStringField("type", "FeatureCollection");
            json.writeNumberField("numberMatched", numberMatched);
            json.writeArrayFieldStart("features");
            json.writeEndArray();

        } catch (NumberFormatException | XMLStreamException e) {
            // TODO: remove XtraserverFrameworkException, instead implement JSON writer for javax.ws.rs exceptions
            throw new ServerErrorException("The WFS did not respond properly to the request.", Response.Status.BAD_GATEWAY);
        }
    }

    @Override
    protected void writeStart(Future<SMInputCursor> rootFuture) throws IOException {
        try {
            this.writeStart(rootFuture.get());
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.getLogger().debug("Unexpected error", ex);
        }
    }

    @Override
    protected void writeEnd() throws IOException {
        json.writeEndObject();
        json.close();
    }


    @Override
    protected void writeFeatureStart(TargetMapping mapping) throws IOException {

    }

    @Override
    protected void writeStartProperties() throws IOException {

    }

    @Override
    protected void writeFeatureEnd() throws IOException {

    }

    @Override
    protected void writeField(TargetMapping mapping, String value) {

    }

    @Override
    protected void writePointGeometry(String x, String y) throws IOException {

    }

    @Override
    protected void writeGeometry(TargetMapping mapping, SMInputCursor feature) {

    }


}
