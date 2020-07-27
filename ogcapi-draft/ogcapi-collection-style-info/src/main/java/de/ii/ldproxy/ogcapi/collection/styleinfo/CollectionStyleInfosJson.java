package de.ii.ldproxy.ogcapi.collection.styleinfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@Provides
@Instantiate
public class CollectionStyleInfosJson implements CollectionStyleInfoFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schema;
    public final static String SCHEMA_REF = "#/components/schemas/StyleInfos";

    public CollectionStyleInfosJson() {
        schema = schemaGenerator.getSchema(StyleInfos.class);
    }

    @Override
    public Response patchStyleInfos(byte[] requestBody, File styleInfosStore, OgcApiApi api, String collectionId) {

        final String apiId = api.getData().getId();
        File apiDir = new File(styleInfosStore + File.separator + apiId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }
        boolean newStyleInfos = isNewStyleInfo(styleInfosStore, api.getId(), collectionId);

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            ObjectReader objectReader;
            StyleInfos updatedContent;
            if (newStyleInfos) {
                updatedContent = mapper.readValue(requestBody, StyleInfos.class);
            } else {
                /* TODO currently treat it like a put, change to proper PATCH
                mapper.readValue(requestBody, StyleInfos.class);
                File metadataFile = new File( styleInfosStore + File.separator + dataset.getId() + File.separator + collectionId);
                StyleInfos currentContent = mapper.readValue(metadataFile, StyleInfos.class);
                objectReader = mapper.readerForUpdating(currentContent);
                updatedContent = objectReader.readValue(requestBody);
                 */
                updatedContent = mapper.readValue(requestBody, StyleInfos.class);
            }
            // parse input for validation
            byte[] updatedContentString = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(updatedContent); // TODO: remove pretty print
            putStylesInfoDocument(styleInfosStore, api.getId(), collectionId, updatedContentString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Response.noContent()
                .build();
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {

        if (path.matches("^/collections/[^//]+/?"))
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(SCHEMA_REF)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();

        throw new IllegalArgumentException("Unexpected path: " + path);
    }

    @Override
    public OgcApiMediaTypeContent getRequestContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {

        // TODO add examples
        if (path.matches("^/collections/[^//]+/?") && method== OgcApiContext.HttpMethods.PATCH)
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schema)
                    .schemaRef(SCHEMA_REF)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new IllegalArgumentException(String.format("Unexpected path/method. Path: %s. Method: %s", path, method));
    }

    private boolean isNewStyleInfo(File styleInfosStore, String apiId, String collectionId) {

        File file = new File( styleInfosStore + File.separator + apiId + File.separator + collectionId);
        return !file.exists();
    }

    /**
     * search for the style in the store and update it, or create a new one
     * @param apiId       the id of the API
     * @param collectionId           the id of the collection, for which a styleInfos document should be updated or created
     * @param payload the new Style as a byte array
     */
    private void putStylesInfoDocument(File styleInfosStore, String apiId, String collectionId, byte[] payload) {

        File styleFile = new File(styleInfosStore + File.separator + apiId + File.separator + collectionId);
        try {
            Files.write(styleFile.toPath(), payload);
        } catch (IOException e) {
            throw new RuntimeException("Could not PATCH style information: " + collectionId);
        }
    }
}
