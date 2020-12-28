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
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wololo.flatgeobuf.generated.Feature;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.google.flatbuffers.Constants.SIZE_PREFIX_LENGTH;

public class FeatureCollectionConversions {

    public static void serialize(SimpleFeatureCollection featureCollection, long featuresCount,
            OutputStream outputStream) throws IOException {

        SimpleFeatureType featureType = featureCollection.getSchema();
        HeaderMeta headerMeta = FeatureTypeConversions.serialize(featureType, featuresCount, outputStream);

        try (FeatureIterator<SimpleFeature> iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                byte[] featureBuffer = FeatureConversions.serialize(feature, headerMeta);
                outputStream.write(featureBuffer);
            }
        }
    }

    public static SimpleFeatureCollection deserialize(ByteBuffer bb) throws IOException {
        HeaderMeta headerMeta = FeatureTypeConversions.deserialize(bb, "testName", "geometryPropertyName");
        int offset = headerMeta.offset;
        SimpleFeatureType ft = headerMeta.featureType;
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(ft);
        MemoryFeatureCollection fc = new MemoryFeatureCollection(ft);
        long count = 0;
        while (bb.hasRemaining()) {
            int featureSize = ByteBufferUtil.getSizePrefix(bb);
            bb.position(offset += SIZE_PREFIX_LENGTH);
            Feature feature = Feature.getRootAsFeature(bb);
            bb.position(offset += featureSize);
            SimpleFeature f = FeatureConversions.deserialize(feature, fb, headerMeta, Long.toString(count++));
            fc.add(f);
        }
        return fc;
    }

}