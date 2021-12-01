/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.maps.domain.MapProvider;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMapProviderTileserver.Builder.class)
public abstract class MapProviderTileserver extends MapProvider {

    public final String getType() { return "TILESERVER"; }

    @Nullable
    public abstract String getUrlTemplate();

    @Override
    public MapProvider mergeInto(MapProvider source) {
        if (Objects.isNull(source) || !(source instanceof MapProviderTileserver))
            return this;

        MapProviderTileserver src = (MapProviderTileserver) source;

        ImmutableMapProviderTileserver.Builder builder = ImmutableMapProviderTileserver.builder()
            .from(src)
            .from(this);

        List<String> tileEncodings = Objects.nonNull(src.getTileEncodings()) ? Lists.newArrayList(src.getTileEncodings()) : Lists.newArrayList();
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        return builder.build();
    }
}
