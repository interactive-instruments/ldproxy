/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;
import akka.japi.function.Function2;
import akka.stream.ActorMaterializer;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.IOResult;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.alpakka.xml.ParseEvent;
import akka.stream.alpakka.xml.javadsl.XmlParsing;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import akka.testkit.javadsl.TestKit;
import akka.util.ByteString;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xtraplatform.feature.transformer.api.EventBasedStreamingFeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.EventBasedStreamingGmlParser;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfigurationOld;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLParser;
import org.codehaus.staxmate.SMInputFactory;
import scala.Tuple2;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class StreamingFeatureTransformerTest {
    static ActorSystem system;
    static ActorMaterializer materializer;
    static LoggingAdapter logger;
    static ObjectMapper mapper;
    static Map<String, FeatureTypeConfigurationOld> mappings;

    //@BeforeClass(groups = {"default"})
    public static void setup() throws IOException {
        final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
                .put("akka.loglevel", "INFO")
                //.put("akka.log-config-on-start", true)
                .build());
        system = ActorSystem.create("test", config);
        materializer = ActorMaterializer.create(system);
        logger = system.log();

        mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setHandlerInstantiator(DynamicTypeIdResolverMock.handlerInstantiator());

        LdProxyService mapping = mapper.readerFor(LdProxyService.class)
                .readValue(new File("/home/zahnen/development/ldproxy/build/data/config-store/ldproxy-services/alkis-sf-xpt3"));

        mappings = mapping.getFeatureTypes();
    }

    //@AfterClass(groups = {"default"})
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    //@Test(groups = {"default"})
    public void test() throws Exception {
        String featureType = "AX_Flurstueck";
        String nameSpace = "http://www.adv-online.de/namespaces/adv/gid/6.0";
        String outputFormat = Gml2GeoJsonMappingProvider.MIME_TYPE;
        FeatureTypeMapping mapping = mappings.get(nameSpace + ":" + featureType).getMappings();
        String count = "10000";
        String page = "1";
        int rounds = 20;

        List<CompletableFuture<Long>> ready = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < rounds; i++) {
            Metrics metrics = runStream(new QName(nameSpace, featureType), mapping, outputFormat, count, page);
            //ready.add(metrics.ready);
            total += metrics.ready.join();
        }

        //long total = all(ready).join().stream().reduce(0L, Long::sum);

        logger.info("median: {}", total/rounds);
    }

    //@Test(groups = {"default"})
    public void testOld() throws Exception {
        String featureType = "AX_Flurstueck";
        String nameSpace = "http://www.adv-online.de/namespaces/adv/gid/6.0";
        String outputFormat = Gml2GeoJsonMappingProvider.MIME_TYPE;
        FeatureTypeMapping mapping = mappings.get(nameSpace + ":" + featureType).getMappings();
        String count = "10000";
        String page = "1";
        int rounds = 20;

        long start = new Date().getTime();
        for (int i = 0; i < rounds; i++) {
            runOldParser(new QName(nameSpace, featureType), mapping, outputFormat, count, page);
        }

        //long total = all(ready).join().stream().reduce(0L, Long::sum);

        logger.info("median: {}", (new Date().getTime() - start)/rounds);
    }

    //@Test(groups = {"default"})
    public void testNewest() throws Exception {
        String featureType = "AX_Flurstueck";
        String nameSpace = "http://www.adv-online.de/namespaces/adv/gid/6.0";
        String outputFormat = Gml2GeoJsonMappingProvider.MIME_TYPE;
        FeatureTypeMapping mapping = mappings.get(nameSpace + ":" + featureType).getMappings();
        String count = "10000";
        String page = "1";
        int rounds = 20;

        //long start = new Date().getTime();
        long total = 0;
        for (int i = 0; i < rounds; i++) {
            Metrics metrics = runNewestParser(new QName(nameSpace, featureType), mapping, outputFormat, count, page);
            //result.toCompletableFuture().get(10, TimeUnit.SECONDS);
            total += metrics.ready.join();//.get(10, TimeUnit.SECONDS);
        }

        //long total = all(ready).join().stream().reduce(0L, Long::sum);

        //logger.info("median: {}", (new Date().getTime() - start)/rounds);
        logger.info("median: {}", total/rounds);
    }

    private Metrics runNewestParser(QName featureType, FeatureTypeMapping mapping, String outputFormat, String count, String page) throws IOException, ExecutionException {
        Source<ByteString, CompletionStage<IOResult>> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count + "-" + page + ".xml"));

        Sink<ByteString, CompletionStage<Done>> ignore = Sink.ignore();
        Sink<ByteString, CompletionStage<Done>> parserGml = GmlStreamParser.consume(featureType, new IgnorableGmlConsumer(logger, false));
        Flow<ByteString, ByteString, CompletionStage<Done>> parserTransform = StreamingGml2GeoJsonFlow.transformer(featureType, mapping);

        return fromFile
                .via(parserTransform)
                .viaMat(new MetricsFlow<>("PARSER"), Keep.right())
                .toMat(Sink.ignore(), Keep.left())
                .run(materializer);
    }

    private Metrics runStream(QName featureType, FeatureTypeMapping mapping, String outputFormat, String count, String page) throws InterruptedException, ExecutionException, TimeoutException {
        logger.info("START");

        Source<ByteString, CompletionStage<IOResult>> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count + "-" + page + ".xml"));//.mapMaterializedValue(nu -> new Date());

        Flow<ByteString, ByteString, NotUsed> mergeBuffer = Flow.of(ByteString.class)
                .conflate((Function2<ByteString, ByteString, ByteString>) ByteString::concat);

        Flow<ByteString, ParseEvent, NotUsed> parserXml = XmlParsing.parser(); // 610
        Flow<ByteString, EventBasedStreamingGmlParser.GmlEvent, NotUsed> parserGml = EventBasedStreamingGmlParser.parser(featureType); // 1203 (610 + 593)
        Flow<ByteString, EventBasedStreamingFeatureTransformer.TransformEvent, NotUsed> parserTransform = EventBasedStreamingFeatureTransformer.parser(featureType, mapping, outputFormat); // 1649 (1203 + 446)
        Flow<EventBasedStreamingFeatureTransformer.TransformEvent, ByteString, Consumer<OutputStream>> writer = GeoJsonFeatureWriter.writer(null); // 1783 (1649 + 134)

        Source<ByteString, Metrics> stream =
                fromFile
                        //fromWfs
                        //.buffer(128, OverflowStrategy.backpressure())
                        .via(parserTransform)
                        .via(writer)
                        .viaMat(new MetricsFlow<>("PARSER"), Keep.right())
                //.watchTermination((start, isDone) -> isDone.thenRun(() -> logger.info("TOOK: {}", new Date().getTime() - start.getTime())))
                //.map(t -> ByteString.fromString(/*t.toString() +*/ "."))
                //.buffer(32768, OverflowStrategy.backpressure())
                //.via(transformer);

                //.via(writer.get())
                //.via(mergeBuffer)
                ;

        return stream.toMat(Sink.ignore(), Keep.left())
                .run(materializer);
                //.thenRun(() -> logger.info("DONE"))
                //.toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private void runOldParser(QName featureType, FeatureTypeMapping mapping, String outputFormat, String count, String page) throws IOException, ExecutionException {
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        final ListenableFuture<InputStream> gml = executorService.submit(() -> new FileInputStream("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count + "-" + page + ".xml"));
        final JsonGenerator jsonGenerator = new JsonFactory().createGenerator(new IgnorableOutputStream()).useDefaultPrettyPrinter().disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);

        GMLAnalyzer analyzer = new de.ii.ldproxy.output.geojson.GeoJsonFeatureWriter(jsonGenerator, mapper, true, mapping, outputFormat, null, new ArrayList<>());
        GMLParser gmlParser = new GMLParser(analyzer, new SMInputFactory(new InputFactoryImpl()));
        gmlParser.parseStream(gml, featureType.getNamespaceURI(), featureType.getLocalPart());
    }

    static <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> futures) {
        CompletableFuture[] cfs = futures.toArray(new CompletableFuture[futures.size()]);

         return CompletableFuture.allOf(cfs)
         .thenApply(aVoid -> futures.stream()
                 .map(CompletableFuture::join)
                 .collect(Collectors.toList()));
    }

    static class Metrics {
        final String name;
        final AtomicLong internalCounter;
        long start;
        long end;
        long upstreamEnd;
        long downstreamEnd;
        CompletableFuture<Long> ready = new CompletableFuture<>();

        Metrics(String name) {
            this.name = name;
            this.internalCounter = new AtomicLong(0);
        }

        long items() {
            return internalCounter.get();
        }

        long duration() {
            return end - start;
        }

        long upstream() {
            return upstreamEnd - start;
        }

        long downstream() {
            return downstreamEnd - start;
        }

        void log() {
            logger.info(name + " metrics: {} items, took {}ms, upstream {}ms, downstream {}ms", items(), duration(), upstream(), downstream());
        }
    }

    static class MetricsFlow<T> extends GraphStageWithMaterializedValue<FlowShape<T, T>, Metrics> {

        private final Inlet<T> in = Inlet.create("MetricsFlow.in");
        private final Outlet<T> out = Outlet.create("MetricsFlow.out");
        private final FlowShape<T, T> shape = FlowShape.of(in, out);
        private final String name;

        MetricsFlow(String name) {
            this.name = name;
        }

        @Override
        public FlowShape<T, T> shape() {
            return shape;
        }

        @Override
        public Tuple2<GraphStageLogic, Metrics> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception {
            // state
            Metrics metrics = new Metrics(name);

            GraphStageLogic graphStageLogic = new GraphStageLogic(shape) {

                {
                    setHandler(in, new AbstractInHandler() {

                        @Override
                        public void onPush() throws Exception {
                            metrics.internalCounter.incrementAndGet();
                            push(out, grab(in));
                        }

                        @Override
                        public void onUpstreamFinish() throws Exception {
                            metrics.upstreamEnd = new Date().getTime();
                            super.onUpstreamFinish();
                        }
                    });

                    setHandler(out, new AbstractOutHandler() {
                        @Override
                        public void onPull() throws Exception {
                            pull(in);
                        }

                        @Override
                        public void onDownstreamFinish() throws Exception {
                            metrics.downstreamEnd = new Date().getTime();
                            super.onDownstreamFinish();
                        }
                    });
                }

                @Override
                public void preStart() throws Exception {
                    metrics.start = new Date().getTime();
                    super.preStart();
                }

                @Override
                public void postStop() throws Exception {
                    metrics.end = new Date().getTime();
                    metrics.log();
                    metrics.ready.complete(metrics.duration());
                    super.postStop();
                }
            };

            return Tuple2.apply(graphStageLogic, metrics);
        }
    }

    static class IgnorableOutputStream extends OutputStream {
        @Override
        public void write(int i) throws IOException {

        }

        @Override
        public void write(byte[] bytes, int i, int i1) throws IOException {

        }
    }

    static class IgnorableGmlConsumer implements GmlConsumer {

        final LoggingAdapter logger;
        final boolean pleaseLog;
        boolean doLog;

        IgnorableGmlConsumer(LoggingAdapter logger, boolean pleaseLog) {
            this.logger = logger;
            this.pleaseLog = pleaseLog;
            this.doLog = pleaseLog;
        }

        //@Override
        public void onStart() {
            if (doLog) logger.info("onGmlStart");
        }

        @Override
        public void onStart(OptionalLong optionalInt, OptionalLong optionalInt1) throws Exception {

        }

        @Override
        public void onEnd() {
            if (doLog) logger.info("onGmlEnd");
        }

        @Override
        public void onFeatureStart(List<String> path) {
            if (doLog) logger.info("onGmlFeatureStart: {}", path);
        }

        @Override
        public void onFeatureEnd(List<String> list) throws Exception {

        }

        //@Override
        public void onFeatureEnd() {
            if (doLog) logger.info("onGmlFeatureEnd");
            doLog = false;
        }

        @Override
        public void onGmlAttribute(String namespace, String localName, List<String> path, String value) {
            if (doLog) logger.info("onGmlAttribute: {}, {}, {}, {}", namespace, localName, value, path);
        }

        @Override
        public void onPropertyStart(List<String> path, List<Integer> multiplicities) {
            if (doLog) logger.info("onGmlPropertyStart: {}, {}", path, multiplicities);
        }

        @Override
        public void onPropertyText(String text) {
            if (doLog) logger.info("onGmlPropertyText: {}", text);
        }

        @Override
        public void onPropertyEnd( List<String> list) throws Exception {

        }

        //@Override
        public void onPropertyEnd(String localName, List<String> path) {
            if (doLog) logger.info("onGmlPropertyEnd: {}, {}", localName, path);
        }

        @Override
        public void onNamespaceRewrite(QName featureType, String namespace) {
            if (doLog) logger.info("onNamespaceRewrite: {}, {}", featureType, namespace);
        }
    }
}
