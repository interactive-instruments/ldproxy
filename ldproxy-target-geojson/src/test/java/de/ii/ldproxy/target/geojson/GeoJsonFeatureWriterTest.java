package de.ii.ldproxy.target.geojson;

import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.testkit.javadsl.TestKit;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import de.ii.ldproxy.output.generic.GenericMappingSubTypeIds;
import de.ii.ldproxy.output.geojson.GeoJsonTargetMappingSubTypeIds;
import de.ii.ldproxy.output.html.MicrodataMappingSubTypeIds;
import de.ii.ldproxy.output.jsonld.JsonLdMappingSubTypeIds;
import de.ii.ogc.wfs.proxy.StreamingFeatureTransformer;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.ExecutionContextExecutor;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.concurrent.CompletionStage;

import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
public class GeoJsonFeatureWriterTest {

    static ActorSystem system;
    static ObjectMapper mapper;

    @BeforeClass(groups = {"default"})
    public static void setup() {
        system = ActorSystem.create();

        mapper = new ObjectMapper();
        mapper.setHandlerInstantiator(new HandlerInstantiator() {
            @Override
            public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
                return null;
            }

            @Override
            public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
                return null;
            }

            @Override
            public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
                return null;
            }

            @Override
            public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
                return null;
            }

            @Override
            public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
                return new DynamicTypeIdResolverMock(new GeoJsonTargetMappingSubTypeIds(), new GenericMappingSubTypeIds(), new MicrodataMappingSubTypeIds(), new JsonLdMappingSubTypeIds());
            }
        });
    }

    @AfterClass(groups = {"default"})
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test(groups = {"default"})
    public void testParser2() throws Exception {
        new TestKit(system) {
            {
                LoggingAdapter log = system.log();
                TestKit probe = new TestKit(system);
                ActorRef requestActor = system.actorOf(SingleRequestInActorExample.props());
                ActorRef writeActor = system.actorOf(FeatureWriterActor.props());

                // TODO: stage that adds mappings to events (or better creates new writer messages)
                // TODO: where to handle json buffering from abstractfeaturewriter
                // TODO: stages that transform writer events to target format (StreamConverters for integration)
                // val inputstream = asInputStream.do().stuff().run()

                // Point
                //requestActor.tell("http://ldproxy02/rest/services/de_gn/NamedPlace/?f=xml", writeActor);
                // LineString
                requestActor.tell("http://ldproxy01/rest/services/topographie/AX_Bahnstrecke/?f=xml", writeActor);
                // MultiSurface
                //requestActor.tell("http://ldproxy02/rest/services/fi_au/AdministrativeUnit/?f=xml", writeActor);

                String response = probe.expectMsgClass(duration("5 seconds"), String.class);

                log.info(response);
                assertEquals("RESPONSE: 200 OK", response);

            }
        };

    }

    //static QName featureType = new QName("http://inspire.ec.europa.eu/schemas/gn/4.0", "NamedPlace");
    static QName featureType = new QName("http://www.adv-online.de/namespaces/adv/gid/6.0", "AX_Bahnstrecke");
    //static QName featureType = new QName("http://inspire.ec.europa.eu/schemas/au/4.0", "AdministrativeUnit");

    static class FeatureWriterActor extends AbstractActor {
        final Http http = Http.get(context().system());
        final ExecutionContextExecutor dispatcher = context().dispatcher();
        final Materializer materializer = ActorMaterializer.create(context());
        final LoggingAdapter log = context().system().log();
        final WfsProxyFeatureTypeMapping featureTypeMapping = mapper.readValue(mapping + mapping2 + mapping3, WfsProxyFeatureTypeMapping.class);

        final Flow<ByteString, StreamingFeatureTransformer.TransformEvent, NotUsed> parser = StreamingFeatureTransformer.parser(featureType, featureTypeMapping, "application/geo+json");

        FeatureWriterActor() throws IOException {
        }

        public static Props props() {
            return Props.create(FeatureWriterActor.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(HttpResponse.class, httpResponse -> {
                        log.info("RESPONSE: " + httpResponse.status());

                        httpResponse.entity().getDataBytes()
                                .via(parser)
                                .runWith(GeoJsonFeatureWriter.writer(new JsonFactory().createGenerator(System.out).useDefaultPrettyPrinter()), materializer);
                                //.runWith(Sink.foreach(event -> log.info(event.toString())), materializer);
                    })
                    .build();
        }

        CompletionStage<HttpResponse> fetch(String url) {
            return http.singleRequest(HttpRequest.create(url));
        }
    }

    static class SingleRequestInActorExample extends AbstractActor {
        final Http http = Http.get(context().system());
        final ExecutionContextExecutor dispatcher = context().dispatcher();
        final Materializer materializer = ActorMaterializer.create(context());
        final LoggingAdapter log = context().system().log();

        public static Props props() {
            return Props.create(SingleRequestInActorExample.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, url -> PatternsCS.pipe(fetch(url), dispatcher).to(getSender()))
                    .build();
        }

        CompletionStage<HttpResponse> fetch(String url) {
            return http.singleRequest(HttpRequest.create(url));
        }
    }

    static String mapping = "{\n" +
            "        \"mappings\" : {\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:AX_Bahnstrecke\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"itemType\" : \"http://schema.org/Place\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"itemType\" : \"http://schema.org/Place\"\n" +
            "            } ]\n" +
            "          },\n" +/*
            "          \"@numberReturned\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"numberReturned\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"type\" : \"META_PROPERTY\"\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"type\" : \"META_PROPERTY\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"type\" : \"META_PROPERTY\"\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"@numberMatched\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"numberMatched\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"type\" : \"META_PROPERTY\"\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"type\" : \"META_PROPERTY\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"type\" : \"META_PROPERTY\"\n" +
            "            } ]\n" +
            "          },\n" +*/
            "          \"http://www.opengis.net/gml/3.2:@id\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"id\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"ID\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"ID\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"ID\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:lebenszeitintervall/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Lebenszeitintervall/http://www.adv-online.de/namespaces/adv/gid/6.0:beginnt\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"lebenszeitintervall.beginnt\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"format\" : \"eeee, d MMMM yyyy[', 'HH:mm:ss[' 'z]]\",\n" +
            "              \"type\" : \"DATE\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"DATE\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:lebenszeitintervall/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Lebenszeitintervall/http://www.adv-online.de/namespaces/adv/gid/6.0:endet\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"lebenszeitintervall.endet\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"format\" : \"eeee, d MMMM yyyy[', 'HH:mm:ss[' 'z]]\",\n" +
            "              \"type\" : \"DATE\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"DATE\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:modellart/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Modellart/http://www.adv-online.de/namespaces/adv/gid/6.0:advStandardModell\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"modellart.advStandardModell\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:modellart/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Modellart/http://www.adv-online.de/namespaces/adv/gid/6.0:sonstigesModell\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"modellart.sonstigesModell\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:anlass\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"anlass\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:zeigtAufExternes/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Fachdatenverbindung/http://www.adv-online.de/namespaces/adv/gid/6.0:art\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"zeigtAufExternes.art\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:zeigtAufExternes/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Fachdatenverbindung/http://www.adv-online.de/namespaces/adv/gid/6.0:fachdatenobjekt/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Fachdatenobjekt/http://www.adv-online.de/namespaces/adv/gid/6.0:name\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"zeigtAufExternes.fachdatenobjekt.name\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:zeigtAufExternes/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Fachdatenverbindung/http://www.adv-online.de/namespaces/adv/gid/6.0:fachdatenobjekt/http://www.adv-online.de/namespaces/adv/gid/6.0:AA_Fachdatenobjekt/http://www.adv-online.de/namespaces/adv/gid/6.0:uri\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"zeigtAufExternes.fachdatenobjekt.uri\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:position\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_GEOMETRY\",\n" +
            "              \"name\" : \"geo\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"GEOMETRY\",\n" +
            "              \"showInCollection\" : false,\n" +
            "              \"geometryType\" : \"LINE_STRING\"\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_GEOMETRY\",\n" +
            "              \"name\" : \"geometry\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"GEOMETRY\",\n" +
            "              \"geometryType\" : \"LINE_STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_GEOMETRY\",\n" +
            "              \"name\" : \"geo\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"GEOMETRY\",\n" +
            "              \"showInCollection\" : false,\n" +
            "              \"geometryType\" : \"LINE_STRING\"\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:bahnkategorie\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"bahnkategorie\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:elektrifizierung\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"elektrifizierung\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:anzahlDerStreckengleise\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"anzahlDerStreckengleise\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:nummerDerBahnstrecke\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"nummerDerBahnstrecke\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:name\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"name\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:zweitname\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"zweitname\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:spurweite\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"spurweite\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n" +
            "          \"http://www.adv-online.de/namespaces/adv/gid/6.0:zustand\" : {\n" +
            "            \"general\" : [ {\n" +
            "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
            "              \"name\" : \"zustand\",\n" +
            "              \"enabled\" : true\n" +
            "            } ],\n" +
            "            \"text/html\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ],\n" +
            "            \"application/geo+json\" : [ {\n" +
            "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\"\n" +
            "            } ],\n" +
            "            \"application/ld+json\" : [ {\n" +
            "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
            "              \"enabled\" : true,\n" +
            "              \"type\" : \"STRING\",\n" +
            "              \"showInCollection\" : true\n" +
            "            } ]\n" +
            "          },\n";
    static String mapping2 =
            "\"http://inspire.ec.europa.eu/schemas/au/4.0:AdministrativeUnit\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"itemType\" : \"http://schema.org/Place\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"itemType\" : \"http://schema.org/Place\"\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://www.opengis.net/gml/3.2:@id\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"id\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"ID\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"ID\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"ID\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:geometry\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_GEOMETRY\",\n" +
                    "              \"name\" : \"geo\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"GEOMETRY\",\n" +
                    "              \"showInCollection\" : false,\n" +
                    "              \"geometryType\" : \"POLYGON\"\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_GEOMETRY\",\n" +
                    "              \"name\" : \"geometry\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"GEOMETRY\",\n" +
                    "              \"geometryType\" : \"MULTI_POLYGON\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_GEOMETRY\",\n" +
                    "              \"name\" : \"geo\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"GEOMETRY\",\n" +
                    "              \"showInCollection\" : false,\n" +
                    "              \"geometryType\" : \"POLYGON\"\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:nationalCode\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"nationalCode\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:inspireId/http://inspire.ec.europa.eu/schemas/base/3.3:Identifier/http://inspire.ec.europa.eu/schemas/base/3.3:localId\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"inspireId.localId\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:inspireId/http://inspire.ec.europa.eu/schemas/base/3.3:Identifier/http://inspire.ec.europa.eu/schemas/base/3.3:namespace\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"inspireId.namespace\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:inspireId/http://inspire.ec.europa.eu/schemas/base/3.3:Identifier/http://inspire.ec.europa.eu/schemas/base/3.3:versionId\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"inspireId.versionId\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:nationalLevelName/http://www.isotc211.org/2005/gmd:LocalisedCharacterString\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"nationalLevelName\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:country/http://www.isotc211.org/2005/gmd:Country\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"country\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:language\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.language\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:sourceOfName\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.sourceOfName\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciation/http://inspire.ec.europa.eu/schemas/gn/4.0:PronunciationOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciationSoundLink\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.pronunciation.pronunciationSoundLink\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciation/http://inspire.ec.europa.eu/schemas/gn/4.0:PronunciationOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciationIPA\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.pronunciation.pronunciationIPA\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:spelling/http://inspire.ec.europa.eu/schemas/gn/4.0:SpellingOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:text\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.spelling.text\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:spelling/http://inspire.ec.europa.eu/schemas/gn/4.0:SpellingOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:script\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.spelling.script\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:spelling/http://inspire.ec.europa.eu/schemas/gn/4.0:SpellingOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:transliterationScheme\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.spelling.transliterationScheme\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:beginLifespanVersion\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"beginLifespanVersion\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"format\" : \"eeee, d MMMM yyyy[', 'HH:mm:ss[' 'z]]\",\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/au/4.0:endLifespanVersion\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"endLifespanVersion\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"format\" : \"eeee, d MMMM yyyy[', 'HH:mm:ss[' 'z]]\",\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n";
    static String mapping3 =
            "\"http://inspire.ec.europa.eu/schemas/gn/4.0:NamedPlace\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"{{Name}}\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"NONE\",\n" +
                    "              \"showInCollection\" : false,\n" +
                    "              \"itemType\" : \"http://schema.org/Place\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"itemType\" : \"http://schema.org/Place\"\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://www.opengis.net/gml/3.2:@id\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"id\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"ID\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"ID\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"ID\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:beginLifespanVersion\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"beginLifespanVersion\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Lifespan (begin)\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"format\" : \"dd/MM/yyyy\",\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:endLifespanVersion\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"endLifespanVersion\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Lifespan (begin)\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"format\" : \"dd/MM/yyyy\",\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"DATE\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:geometry\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_GEOMETRY\",\n" +
                    "              \"name\" : \"geo\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"GEOMETRY\",\n" +
                    "              \"showInCollection\" : false,\n" +
                    "              \"geometryType\" : \"GENERIC\"\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_GEOMETRY\",\n" +
                    "              \"name\" : \"geometry\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"GEOMETRY\",\n" +
                    "              \"geometryType\" : \"GENERIC\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_GEOMETRY\",\n" +
                    "              \"name\" : \"geo\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"GEOMETRY\",\n" +
                    "              \"showInCollection\" : false,\n" +
                    "              \"geometryType\" : \"GENERIC\"\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:inspireId/http://inspire.ec.europa.eu/schemas/base/3.3:Identifier/http://inspire.ec.europa.eu/schemas/base/3.3:localId\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"inspireId.localId\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"INSPIRE ID (local ID)\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:inspireId/http://inspire.ec.europa.eu/schemas/base/3.3:Identifier/http://inspire.ec.europa.eu/schemas/base/3.3:namespace\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"inspireId.namespace\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"INSPIRE ID (namespace)\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:inspireId/http://inspire.ec.europa.eu/schemas/base/3.3:Identifier/http://inspire.ec.europa.eu/schemas/base/3.3:versionId\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"inspireId.versionId\",\n" +
                    "              \"enabled\" : false,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:localType/http://www.isotc211.org/2005/gmd:LocalisedCharacterString\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"localType\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : true\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Type of place\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:language\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.language\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Language\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:sourceOfName\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.sourceOfName\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Source\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciation/http://inspire.ec.europa.eu/schemas/gn/4.0:PronunciationOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciationSoundLink\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.pronunciation.pronunciationSoundLink\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciation/http://inspire.ec.europa.eu/schemas/gn/4.0:PronunciationOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:pronunciationIPA\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.pronunciation.pronunciationIPA\",\n" +
                    "              \"enabled\" : false,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:spelling/http://inspire.ec.europa.eu/schemas/gn/4.0:SpellingOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:text\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.spelling.text\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : true\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Name\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:spelling/http://inspire.ec.europa.eu/schemas/gn/4.0:SpellingOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:script\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.spelling.script\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"name\" : \"Script\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          },\n" +
                    "          \"http://inspire.ec.europa.eu/schemas/gn/4.0:name/http://inspire.ec.europa.eu/schemas/gn/4.0:GeographicalName/http://inspire.ec.europa.eu/schemas/gn/4.0:spelling/http://inspire.ec.europa.eu/schemas/gn/4.0:SpellingOfName/http://inspire.ec.europa.eu/schemas/gn/4.0:transliterationScheme\" : {\n" +
                    "            \"general\" : [ {\n" +
                    "              \"mappingType\" : \"GENERIC_PROPERTY\",\n" +
                    "              \"name\" : \"name.spelling.transliterationScheme\",\n" +
                    "              \"enabled\" : false,\n" +
                    "              \"filterable\" : false\n" +
                    "            } ],\n" +
                    "            \"text/html\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ],\n" +
                    "            \"application/geo+json\" : [ {\n" +
                    "              \"mappingType\" : \"GEO_JSON_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\"\n" +
                    "            } ],\n" +
                    "            \"application/ld+json\" : [ {\n" +
                    "              \"mappingType\" : \"MICRODATA_PROPERTY\",\n" +
                    "              \"enabled\" : true,\n" +
                    "              \"filterable\" : false,\n" +
                    "              \"type\" : \"STRING\",\n" +
                    "              \"showInCollection\" : true\n" +
                    "            } ]\n" +
                    "          }" +
                    "        }\n" +
                    "      }";
}