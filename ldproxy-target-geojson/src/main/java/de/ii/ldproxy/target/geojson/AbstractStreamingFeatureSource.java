package de.ii.ldproxy.target.geojson;

import akka.stream.*;
import akka.stream.stage.*;
import akka.util.ByteString;
import akka.util.ByteString.ByteStrings;
import de.ii.ogc.wfs.proxy.StreamingFeatureTransformer.TransformEvent;
import scala.Tuple2;
import scala.concurrent.ExecutionContext;

import java.io.IOException;
import java.io.OutputStream;
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
