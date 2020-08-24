/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import akka.stream.Attributes;
import akka.stream.Outlet;
import akka.stream.SourceShape;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;

import java.util.function.Consumer;

/**
 * @author zahnen
 */
public abstract class AbstractStreamingFeatureSource extends GraphStage<SourceShape<ByteString>> {

    private final Outlet<ByteString> out = Outlet.create("AbstractStreamingFeatureSource.out");
    private final SourceShape<ByteString> shape = SourceShape.of(out);

    @Override
    public SourceShape<ByteString> shape() {
        return shape;
    }

    protected abstract void initalize(Consumer<ByteString> emit);

    protected abstract void onSourcePull();

    @Override
    public GraphStageLogic createLogic(Attributes inheritedAttributes) throws Exception {

        return new GraphStageLogic(shape) {
            {
                setHandler(out, new AbstractOutHandler() {
                    @Override
                    public void onPull() throws Exception {
                        onSourcePull();
                    }
                });
            }

            @Override
            public void preStart() {
                Consumer<ByteString> doEmit = byteString -> emit(out, byteString);
                initalize(doEmit);
            }
        };
    }
}
