/*
Copyright (c) 2018, Björn Harrtell

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.wololo.flatgeobuf.geotools;

import com.google.flatbuffers.ByteBufferUtil;
import com.google.flatbuffers.FlatBufferBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.wololo.flatgeobuf.Constants;
import org.wololo.flatgeobuf.generated.Column;
import org.wololo.flatgeobuf.generated.ColumnType;
import org.wololo.flatgeobuf.generated.GeometryType;
import org.wololo.flatgeobuf.generated.Header;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;

import static com.google.flatbuffers.Constants.SIZE_PREFIX_LENGTH;

public class FeatureTypeConversions {

    public static HeaderMeta serialize(SimpleFeatureType featureType, long featuresCount, OutputStream outputStream)
            throws IOException {

        List<AttributeDescriptor> types = featureType.getAttributeDescriptors();
        List<ColumnMeta> columns = new ArrayList<ColumnMeta>();

        for (int i = 0; i < types.size(); i++) {
            AttributeDescriptor ad = types.get(i);
            if (ad instanceof GeometryDescriptor) {
                // multiple geometries per feature is not supported
            } else {
                String key = ad.getLocalName();
                Class<?> binding = ad.getType().getBinding();
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
                else
                    throw new RuntimeException("Unknown type");
                columns.add(column);
            }
        }

        byte geometryType = GeometryType.Unknown;
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        if (geometryDescriptor != null)
            geometryType = GeometryConversions
                    .toGeometryType(featureType.getGeometryDescriptor().getType().getBinding());

        outputStream.write(Constants.MAGIC_BYTES);

        HeaderMeta headerMeta = new HeaderMeta();
        headerMeta.featuresCount = featuresCount;
        headerMeta.geometryType = geometryType;
        headerMeta.columns = columns;

        byte[] headerBuffer = buildHeader(headerMeta);
        outputStream.write(headerBuffer);

        return headerMeta;
    }

    public static byte[] buildHeader(HeaderMeta headerMeta) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int[] columnsArray = headerMeta.columns.stream().mapToInt(c -> {
            int nameOffset = builder.createString(c.name);
            int type = c.type;
            return Column.createColumn(builder, nameOffset, type, 0, 0, -1, -1, -1, true, false, false, 0);
        }).toArray();
        int columnsOffset = Header.createColumnsVector(builder, columnsArray);

        Header.startHeader(builder);
        Header.addGeometryType(builder, headerMeta.geometryType);
        Header.addIndexNodeSize(builder, 0);
        Header.addColumns(builder, columnsOffset);
        Header.addFeaturesCount(builder, headerMeta.featuresCount);
        int offset = Header.endHeader(builder);

        builder.finishSizePrefixed(offset);

        return builder.sizedByteArray();
    }

    public static HeaderMeta deserialize(ByteBuffer bb, String name, String geometryPropertyName) throws IOException {
        int offset = 0;
        if (Constants.isFlatgeobuf(bb))
            throw new IOException("This is not a flatgeobuf!");
        bb.position(offset += Constants.MAGIC_BYTES.length);
        int headerSize = ByteBufferUtil.getSizePrefix(bb);
        bb.position(offset += SIZE_PREFIX_LENGTH);
        Header header = Header.getRootAsHeader(bb);
        bb.position(offset += headerSize);
        int geometryType = header.geometryType();
        Class<?> geometryClass;
        switch (geometryType) {
        case GeometryType.Unknown:
            geometryClass = Geometry.class;
            break;
        case GeometryType.Point:
            geometryClass = Point.class;
            break;
        case GeometryType.MultiPoint:
            geometryClass = MultiPoint.class;
            break;
        case GeometryType.LineString:
            geometryClass = LineString.class;
            break;
        case GeometryType.MultiLineString:
            geometryClass = MultiLineString.class;
            break;
        case GeometryType.Polygon:
            geometryClass = Polygon.class;
            break;
        case GeometryType.MultiPolygon:
            geometryClass = MultiPolygon.class;
            break;
        default:
            throw new RuntimeException("Unknown geometry type");
        }

        int columnsLength = header.columnsLength();
        ArrayList<ColumnMeta> columnMetas = new ArrayList<ColumnMeta>();
        for (int i = 0; i < columnsLength; i++) {
            ColumnMeta columnMeta = new ColumnMeta();
            columnMeta.name = header.columns(i).name();
            columnMeta.type = (byte) header.columns(i).type();
            columnMetas.add(columnMeta);
        }

        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName(name);
        ftb.add(geometryPropertyName, geometryClass);
        for (ColumnMeta columnMeta : columnMetas)
            ftb.add(columnMeta.name, columnMeta.getBinding());
        SimpleFeatureType ft = ftb.buildFeatureType();

        HeaderMeta headerMeta = new HeaderMeta();
        headerMeta.columns = columnMetas;
        headerMeta.geometryType = (byte) geometryType;
        headerMeta.offset = offset;
        headerMeta.featureType = ft;

        return headerMeta;
    }

}
