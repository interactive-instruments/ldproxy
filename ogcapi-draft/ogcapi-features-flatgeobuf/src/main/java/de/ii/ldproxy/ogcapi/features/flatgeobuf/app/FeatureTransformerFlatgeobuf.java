/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.flatgeobuf.app;

import com.google.common.collect.ImmutableMap;
import com.google.flatbuffers.FlatBufferBuilder;
import de.ii.ldproxy.ogcapi.features.flatgeobuf.domain.FeaturesFormatFlatgeobuf;
import de.ii.ldproxy.ogcapi.features.flatgeobuf.domain.FlatgeobufConfiguration;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.flatgeobuf.Constants;
import org.wololo.flatgeobuf.generated.ColumnType;
import org.wololo.flatgeobuf.generated.Feature;
import org.wololo.flatgeobuf.generated.Geometry;
import org.wololo.flatgeobuf.generated.GeometryType;
import org.wololo.flatgeobuf.geotools.ColumnMeta;
import org.wololo.flatgeobuf.geotools.FeatureTypeConversions;
import org.wololo.flatgeobuf.geotools.GeometryConversions;
import org.wololo.flatgeobuf.geotools.GeometryOffsets;
import org.wololo.flatgeobuf.geotools.HeaderMeta;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public class FeatureTransformerFlatgeobuf extends FeatureTransformerSimpleFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerFlatgeobuf.class);

    private final Map<String,Class> properties;
    private HeaderMeta headerMeta;

    public FeatureTransformerFlatgeobuf(ImmutableFeatureTransformationContextFlatgeobuf transformationContext) {
        super(FlatgeobufConfiguration.class,
              transformationContext.getApiData(), transformationContext.getCollectionId(),
              transformationContext.getCodelists(), transformationContext.getServiceUrl(),
              transformationContext.isFeatureCollection(), transformationContext.getOutputStream(),
              transformationContext.getCrsTransformer().orElse(null), transformationContext.shouldSwapCoordinates(),
              transformationContext.getFields(), ImmutableMap.of());
        this.properties = transformationContext.getSimpleFeatureType();
    }

    @Override
    public String getTargetFormat() {
        return FeaturesFormatFlatgeobuf.MEDIA_TYPE.type().toString();
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws IOException {
        this.headerMeta = serialize(collectionId, properties, numberReturned.getAsLong(), outputStream);
    }

    public static HeaderMeta serialize(String typeName, Map<String,Class> properties, long featuresCount, OutputStream outputStream)
            throws IOException {

        List<ColumnMeta> columns = new ArrayList<ColumnMeta>();

        byte[] geometryType = {GeometryType.Unknown};
        properties.entrySet()
                  .stream()
                  .forEach(entry -> {
                      String key = entry.getKey();
                      Class<?> binding = entry.getValue();
                      ColumnMeta column = new ColumnMeta();
                      column.name = key;
                      if (binding.isAssignableFrom(Boolean.class))
                          column.type = ColumnType.Bool;
                      else if (binding.isAssignableFrom(Byte.class))
                          column.type = ColumnType.Byte;
                      else if (binding.isAssignableFrom(Short.class))
                          column.type = ColumnType.Short;
                      else if (binding.isAssignableFrom(Integer.class))
                          column.type = ColumnType.Int;
                      else if (binding.isAssignableFrom(BigInteger.class))
                          column.type = ColumnType.Long;
                      else if (binding.isAssignableFrom(BigDecimal.class))
                          column.type = ColumnType.Double;
                      else if (binding.isAssignableFrom(Long.class))
                          column.type = ColumnType.Long;
                      else if (binding.isAssignableFrom(Double.class))
                          column.type = ColumnType.Double;
                      else if (binding.isAssignableFrom(LocalDateTime.class) || binding.isAssignableFrom(LocalDate.class)
                              || binding.isAssignableFrom(LocalTime.class) || binding.isAssignableFrom(OffsetDateTime.class)
                              || binding.isAssignableFrom(OffsetTime.class))
                          column.type = ColumnType.DateTime;
                      else if (binding.isAssignableFrom(String.class))
                          column.type = ColumnType.String;
                      else if (binding.isAssignableFrom(Point.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.Point;
                      } else if (binding.isAssignableFrom(MultiPoint.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.MultiPoint;
                      } else if (binding.isAssignableFrom(LineString.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.LineString;
                      } else if (binding.isAssignableFrom(MultiLineString.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.MultiLineString;
                      } else if (binding.isAssignableFrom(Polygon.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.Polygon;
                      } else if (binding.isAssignableFrom(MultiPolygon.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.MultiPolygon;
                      } else if (binding.isAssignableFrom(GeometryCollection.class)) {
                          column.type = Byte.MIN_VALUE;
                          geometryType[0] = GeometryType.GeometryCollection;
                      } if (column.type != Byte.MIN_VALUE)
                          columns.add(column);
                  });

        outputStream.write(Constants.MAGIC_BYTES);

        HeaderMeta headerMeta = new HeaderMeta();
        headerMeta.featuresCount = featuresCount;
        headerMeta.geometryType = geometryType[0];
        headerMeta.columns = columns;

        byte[] headerBuffer = FeatureTypeConversions.buildHeader(headerMeta);
        outputStream.write(headerBuffer);

        return headerMeta;
    }

    @Override
    public void onEnd() throws IOException {
    }

    @Override
    public void onFeatureEnd() throws IOException {
        byte[] featureBuffer = serializeFeature(currentGeometry, currentId, currentProperties, headerMeta);
        outputStream.write(featureBuffer);
    }

    private static void writeString(ByteBuffer bb, String value) {
        byte[] stringBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
        bb.putInt(stringBytes.length);
        bb.put(stringBytes);
    }

    private static byte[] serializeFeature(org.locationtech.jts.geom.Geometry geometry, String id, Map<String, Object> properties, HeaderMeta headerMeta) throws IOException {
        // TODO id
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        ByteBuffer bb = ByteBuffer.allocate(1024 * 1024);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short i = 0; i < headerMeta.columns.size(); i++) {
            ColumnMeta column = headerMeta.columns.get(i);
            byte type = column.type;
            Object value = properties.get(column.name);
            if (value == null)
                continue;
            bb.putShort(i);
            if (type == ColumnType.Bool)
                bb.put((byte) ((boolean) value ? 1 : 0));
            else if (type == ColumnType.Byte)
                bb.put((byte) value);
            else if (type == ColumnType.Short)
                bb.putShort((short) value);
            else if (type == ColumnType.Int)
                if (value instanceof Long)
                    bb.putInt(((Long)value).intValue());
                else
                    bb.putInt((int) value);
            else if (type == ColumnType.Long)
                if (value instanceof Long)
                    bb.putLong((long) value);
                else if (value instanceof BigInteger)
                    bb.putLong(((BigInteger) value).longValue());
                else
                    bb.putLong((long) value);
            else if (type == ColumnType.Double)
                if (value instanceof Double)
                    bb.putDouble((double) value);
                else if (value instanceof BigDecimal)
                    bb.putDouble(((BigDecimal) value).doubleValue());
                else
                    bb.putDouble((double) value);
            else if (type == ColumnType.DateTime) {
                String isoDateTime = "";
                if (value instanceof LocalDateTime)
                    isoDateTime = ((LocalDateTime) value).toString();
                else if (value instanceof LocalDate)
                    isoDateTime = ((LocalDate) value).toString();
                else if (value instanceof LocalTime)
                    isoDateTime = ((LocalTime) value).toString();
                else if (value instanceof OffsetDateTime)
                    isoDateTime = ((OffsetDateTime) value).toString();
                else if (value instanceof OffsetTime)
                    isoDateTime = ((OffsetTime) value).toString();
                else
                    throw new RuntimeException("Unknown date/time type " + type);
                writeString(bb, isoDateTime);
            } else if (type == ColumnType.String)
                writeString(bb, (String) value);
            else
                throw new RuntimeException("Unknown type " + type);
        }

        int propertiesOffset = 0;
        if (bb.position() > 0) {
            byte[] data = Arrays.copyOfRange(bb.array(), 0, bb.position());
            propertiesOffset = Feature.createPropertiesVector(builder, data);
        }
        GeometryOffsets go = GeometryConversions.serialize(builder, geometry, headerMeta.geometryType);
        int geometryOffset = 0;
        if (go.gos != null && go.gos.length > 0) {
            int[] partOffsets = new int[go.gos.length];
            for (int i = 0; i < go.gos.length; i++) {
                GeometryOffsets goPart = go.gos[i];
                int partOffset = Geometry.createGeometry(builder, goPart.endsOffset, goPart.coordsOffset, 0, 0, 0, 0, 0,
                                                         0);
                partOffsets[i] = partOffset;
            }
            int partsOffset = Geometry.createPartsVector(builder, partOffsets);
            geometryOffset = Geometry.createGeometry(builder, 0, 0, 0, 0, 0, 0, 0, partsOffset);
        } else {
            geometryOffset = Geometry.createGeometry(builder, go.endsOffset, go.coordsOffset, 0, 0, 0, 0, 0, 0);
        }
        int featureOffset = Feature.createFeature(builder, geometryOffset, propertiesOffset, 0);
        builder.finishSizePrefixed(featureOffset);

        return builder.sizedByteArray();
    }
}
