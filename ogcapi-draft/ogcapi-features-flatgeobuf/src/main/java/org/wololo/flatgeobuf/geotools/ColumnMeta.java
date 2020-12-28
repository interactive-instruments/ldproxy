/*
Copyright (c) 2018, Björn Harrtell

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.wololo.flatgeobuf.geotools;

import org.wololo.flatgeobuf.generated.ColumnType;

public class ColumnMeta {
    public String name;
    public byte type;

    public Class<?> getBinding() {
        switch (type) {
        case ColumnType.Bool:
            return Boolean.class;
        case ColumnType.Byte:
            return Byte.class;
        case ColumnType.Short:
            return Short.class;
        case ColumnType.Int:
            return Integer.class;
        case ColumnType.Long:
            return Long.class;
        case ColumnType.Double:
            return Double.class;
        case ColumnType.DateTime:
            return String.class;
        case ColumnType.String:
            return String.class;
        default:
            throw new RuntimeException("Unknown type");
        }
    }
}