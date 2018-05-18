/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.gml2json.AbstractFeatureWriter;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author fischer
 */
// TODO: implement AbstractHitsWriter
public class GeoJsonHitsWriter extends AbstractFeatureWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonHitsWriter.class);

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
            LOGGER.debug("Unexpected error", ex);
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
    protected void writeField(TargetMapping mapping, String value, int occurrence) {

    }

    @Override
    protected void writePointGeometry(String x, String y) throws IOException {

    }

    @Override
    protected void writeGeometry(TargetMapping mapping, SMInputCursor feature) {

    }


}
