/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import akka.util.ByteString;
import akka.util.ByteString.ByteStrings;
import de.ii.xtraplatform.feature.transformer.api.EventBasedStreamingFeatureTransformer;
import scala.Tuple2;
import scala.concurrent.ExecutionContext;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public abstract class AbstractStreamingFeatureWriter extends GraphStageWithMaterializedValue<FlowShape<EventBasedStreamingFeatureTransformer.TransformEvent, ByteString>, Consumer<OutputStream>> {

    private final Inlet<EventBasedStreamingFeatureTransformer.TransformEvent> in = Inlet.create("AbstractStreamingFeatureWriter.in");
    private final Outlet<ByteString> out = Outlet.create("AbstractStreamingFeatureWriter.out");
    private final FlowShape<EventBasedStreamingFeatureTransformer.TransformEvent, ByteString> shape = FlowShape.of(in, out);

    @Override
    public FlowShape<EventBasedStreamingFeatureTransformer.TransformEvent, ByteString> shape() {
        return shape;
    }

    protected abstract void writeEvent(final EventBasedStreamingFeatureTransformer.TransformEvent transformEvent, Runnable onComplete, ExecutionContext executionContext) throws IOException;

    //protected abstract void setOutputStream(final OutputStream outputStream);

    protected abstract void initalize(OutputStream outputStream, Consumer<ByteString> push);

    Consumer<ByteString> doPush;
    OutputStream outputStream = null;


    @Override
    public Tuple2<GraphStageLogic, Consumer<OutputStream>> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception {
        GraphStageLogic graphStageLogic = new GraphStageLogic(shape) {

            // state

            {
                //ActorMaterializer actorMaterializer = ActorMaterializerHelper.downcast(materializer());
                //ExecutionContext dispatcher = actorMaterializer.system().dispatchers().lookup(actorMaterializer.settings().dispatcher()/*.blockingIoDispatcher()*/);

                setHandler(in, new AbstractInHandler() {

                    @Override
                    public void onPush() throws Exception {
                        final EventBasedStreamingFeatureTransformer.TransformEvent transformEvent = grab(in);

                        writeEvent(transformEvent, () -> completeStage(), materializer().executionContext());

                        if (!isClosed(in))
                            pull(in);
                    }
                });

                setHandler(out, new AbstractOutHandler() {
                    @Override
                    public void onPull() throws Exception {
                        if (!hasBeenPulled(in))
                        pull(in);
                    }
                });
            }

            @Override
            public void preStart() {
                // initiate the flow of data by issuing a first pull on materialization:
              //  pull(in);
                outputStream = new OutputStream() {
                    @Override
                    public void write(int i) throws IOException {

                    }

                    @Override
                    public void write(byte[] bytes, int i, int i1) throws IOException {
                        push(out, ByteStrings.fromArray(bytes, i, i1));
                    }
                };

                doPush = byteString -> emit(out, byteString);

                initalize(outputStream, doPush);
            }
        };

        return Tuple2.apply(graphStageLogic, (Consumer<OutputStream>) o -> {});
    }

    //@Override
    //public GraphStageLogic createLogic(Attributes inheritedAttributes) throws Exception {
    //    return
    //}
}
