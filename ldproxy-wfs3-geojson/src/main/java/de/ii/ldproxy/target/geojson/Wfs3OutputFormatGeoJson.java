package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3MediaType;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClasses;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatGeoJson implements Wfs3ConformanceClass, Wfs3OutputFormatExtension {

    private static final Wfs3MediaType MEDIA_TYPE = ImmutableWfs3MediaType.builder()
                                                                          .main(new MediaType("application", "geo+json"))
                                                                          .label("GeoJSON")
                                                                          .metadata(MediaType.APPLICATION_JSON_TYPE)
                                                                          .build();

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/wfs-1/3.0/req/geojson";
    }

    @Override
    public Wfs3MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getConformanceResponse(List<Wfs3ConformanceClass> wfs3ConformanceClasses, String serviceLabel, Wfs3MediaType wfs3MediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix) {
        return response(new Wfs3ConformanceClasses(wfs3ConformanceClasses.stream()
                                                                         .map(Wfs3ConformanceClass::getConformanceClass)
                                                                         .collect(Collectors.toList())));
    }

    @Override
    public Response getDatasetResponse(Wfs3Collections wfs3Collections, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String staticUrlPrefix, boolean isCollections) {
        return response(wfs3Collections);
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName) {
        return response(wfs3Collection);
    }

    @Override
    public Response getItemsResponse(Wfs3ServiceData serviceData, Wfs3MediaType mediaType, Wfs3MediaType[] alternativeMediaTypes, URICustomizer uriCustomizer, String collectionName, FeatureQuery query, FeatureStream<FeatureTransformer> featureTransformStream, CrsTransformer crsTransformer, String staticUrlPrefix, FeatureStream<GmlConsumer> featureStream) {
        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        int pageSize = query.getLimit();
        int page = pageSize > 0 ? (pageSize + query.getOffset()) / pageSize : 0;
        boolean isCollection = uriCustomizer.isLastPathSegment("items");

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriCustomizer, isCollection, page, pageSize, mediaType, alternativeMediaTypes);


        return response(stream(featureTransformStream, outputStream -> new FeatureTransformerGeoJson(createJsonGenerator(outputStream), isCollection, crsTransformer, links, pageSize, uriCustomizer.copy()
                                                                                                                                                                                                    .cutPathAfterSegments(serviceData.getId())
                                                                                                                                                                                                    .clearParameters()
                                                                                                                                                                                                    .toString(), query.getMaxAllowableOffset())), MEDIA_TYPE.main()
                                                                                                                                                                                                                                                            .toString());
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.of(new Gml2GeoJsonMappingProvider());
    }

    private Response response(Object entity) {
        return response(entity, null);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }

    // TODO: same for every Wfs3OutputFormat, extract
    private StreamingOutput stream(FeatureStream<FeatureTransformer> featureTransformStream, final Function<OutputStream, FeatureTransformer> featureTransformer) {
        return outputStream -> {
            try {
                featureTransformStream.apply(featureTransformer.apply(outputStream))
                                      .toCompletableFuture()
                                      .join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error", e.getCause());
            }
        };
    }

    // TODO: move somewhere else
    public JsonGenerator createJsonGenerator(OutputStream output) {
        JsonGenerator json = null;
        try {
            json = new JsonFactory().createGenerator(output);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        json.setCodec(new ObjectMapper());
        //if (useFormattedJsonOutput) {
        json.useDefaultPrettyPrinter();
        //}
        // Zum JSON debuggen hier einschalten.
        //JsonGenerator jsond = new JsonGeneratorDebug(json);
        return json;
    }
}
