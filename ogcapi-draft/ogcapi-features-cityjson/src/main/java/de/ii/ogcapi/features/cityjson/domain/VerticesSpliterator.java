/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.domain;

import java.util.Spliterator;
import java.util.function.Consumer;

public class VerticesSpliterator implements Spliterator<long[]> {
    private static final int SPLIT_SIZE = 10_000;
    private final Vertices vertices;
    private final int size;
    private final int spliteratorStartIdx;
    private int spliteratorEndIdx;
    private int idx;

    VerticesSpliterator(Vertices vertices) {
        assert vertices.isLocked();
        this.vertices = vertices;
        this.size = vertices.getSize();
        this.spliteratorStartIdx = 0;
        this.spliteratorEndIdx = size-1;
        this.idx = this.spliteratorStartIdx;
    }

    VerticesSpliterator(Vertices vertices, int spliteratorStartIdx, int spliteratorEndIdx) {
        assert vertices.isLocked();
        this.vertices = vertices;
        this.size = vertices.getSize();
        this.spliteratorStartIdx = spliteratorStartIdx;
        this.spliteratorEndIdx = spliteratorEndIdx;
        this.idx = this.spliteratorStartIdx;
    }

    @Override
    public boolean tryAdvance(Consumer<? super long[]> action) {
        if (idx <= spliteratorEndIdx) {
            action.accept(vertices.getVertex(idx));
            idx++;
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<long[]> trySplit() {
        if (spliteratorEndIdx-idx > SPLIT_SIZE) {
            int splitIdx = idx / 2 + spliteratorEndIdx / 2;
            VerticesSpliterator split = new VerticesSpliterator(vertices, splitIdx + 1, spliteratorEndIdx);
            spliteratorEndIdx = splitIdx;
            return split;
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return spliteratorEndIdx-spliteratorStartIdx+1;
    }

    @Override
    public int characteristics() {
        return ORDERED + SIZED + SUBSIZED + NONNULL + IMMUTABLE;
    }
}
