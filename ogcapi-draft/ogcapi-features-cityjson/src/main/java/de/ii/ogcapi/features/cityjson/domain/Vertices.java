/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.domain;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

public class Vertices {

    private static final Logger LOGGER = LoggerFactory.getLogger(Vertices.class);
    private final static int SIZE = 1000;
    List<long[][]> globalVertices;
    int blockCount;
    long[][] currentVertices;
    int count;
    Map<VertexKey, Integer> currentIndex;
    boolean locked;

    public Vertices() {
        globalVertices = new ArrayList<>();
        blockCount = 0;
        currentVertices = new long[SIZE][3];
        count = 0;
        currentIndex = new HashMap<>(SIZE);
        locked = false;
    }

    public boolean isLocked() {
        return locked;
    }

    public int getSize() {
        return blockCount*SIZE + count;
    }

    public long[] getVertex(int idx) {
        assert locked;
        return globalVertices.get(idx / SIZE)[idx % SIZE];
        /* for unlocked state, the code would be:
        return (idx / SIZE < globalVertices.size())
            ? globalVertices.get(idx / SIZE)[idx % SIZE]
            : currentVertices[idx % SIZE];
         */
    }

    public void lock() {
        locked = true;
        globalVertices.add(currentVertices);
    }

    public int addVertex(long... xyz) {
        assert !locked;
        assert xyz.length==3;
        VertexKey key = new VertexKey(xyz);
        Integer idx = currentIndex.get(key);
        if (Objects.isNull(idx)) {
            if (count==SIZE) {
                globalVertices.add(currentVertices);
                blockCount++;
                currentVertices = new long[SIZE][3];
                count = 0;
            }
            currentVertices[count] = Arrays.copyOf(xyz,3);
            idx = blockCount*SIZE + count++;
            currentIndex.put(key, idx);
        } else {
            long[] xyz2 = idx / SIZE < globalVertices.size()
                ? globalVertices.get(idx / SIZE)[idx % SIZE]
                : currentVertices[idx % SIZE];
            if (xyz[0]!=xyz2[0] || xyz[1]!=xyz2[1] || xyz[2]!=xyz2[2]) {
                LOGGER.error("Collision: {} vs {}.", xyz, xyz2);
            }
        }
        return idx;
    }

    private static class VertexKey {
        final long x;
        final long y;
        final long z;

        VertexKey(long... vertex) {
            assert vertex.length==3;
            x = vertex[0];
            y = vertex[1];
            z = vertex[2];
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 31)
                .append(x)
                .append(y)
                .append(z)
                .toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof VertexKey && ((VertexKey) obj).x==x && ((VertexKey) obj).y==y && ((VertexKey) obj).z==z;
        }
    }

    public Optional<long[]> getBoundingBox() {
        assert locked;
        Optional<long[]> min = StreamSupport.stream(new VerticesSpliterator(this), true)
            .reduce((vertex1, vertex2) -> new long[]{
                Math.min(vertex1[0], vertex2[0]),
                Math.min(vertex1[1], vertex2[1]),
                Math.min(vertex1[2], vertex2[2])});
        Optional<long[]> max = StreamSupport.stream(new VerticesSpliterator(this), true)
            .reduce((vertex1, vertex2) -> new long[]{
                Math.max(vertex1[0], vertex2[0]),
                Math.max(vertex1[1], vertex2[1]),
                Math.max(vertex1[2], vertex2[2])});

        if (min.isEmpty() || max.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(LongStream.concat(Arrays.stream(min.get()), Arrays.stream(max.get())).toArray());
    }

}
