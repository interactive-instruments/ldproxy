package de.ii.ldproxy.ogcapi.collections.styleinfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.json.domain.JsonConfiguration;
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
import java.nio.file.Path;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class CollectionStyleInfoFormatJson implements CollectionStyleInfoFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schema;
    public final static String SCHEMA_REF = "#/components/schemas/StyleInfo";

    public CollectionStyleInfoFormatJson() {
        schema = schemaGenerator.getSchema(StyleInfo.class);
    }

    @Override
    public Response patchStyleInfos(byte[] requestBody, Path styleInfosStore, OgcApi api, String collectionId) throws IOException {

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

        ObjectReader objectReader;
        StyleInfo updatedContent;
        if (newStyleInfos) {
            updatedContent = mapper.readValue(requestBody, StyleInfo.class);
        } else {
            /* TODO currently treat it like a put, change to proper PATCH
            mapper.readValue(requestBody, StyleInfo.class);
            File metadataFile = new File( styleInfosStore + File.separator + dataset.getId() + File.separator + collectionId);
            StyleInfo currentContent = mapper.readValue(metadataFile, StyleInfo.class);
            objectReader = mapper.readerForUpdating(currentContent);
            updatedContent = objectReader.readValue(requestBody);
             */
            updatedContent = mapper.readValue(requestBody, StyleInfo.class);
        }
        // parse input for validation
        Optional<JsonConfiguration> jsonConfig = api.getData()
                                                    .getExtension(JsonConfiguration.class);
        byte[] updatedContentString = jsonConfig.isPresent() && jsonConfig.get().getUseFormattedJsonOutput() ?
                mapper.writerWithDefaultPrettyPrinter()
                      .writeValueAsBytes(updatedContent) :
                mapper.writeValueAsBytes(updatedContent);
        putStylesInfoDocument(styleInfosStore, api.getId(), collectionId, updatedContentString);

        return Response.noContent()
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        if (path.matches("^/collections/[^//]+/?"))
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(SCHEMA_REF)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {

        // TODO add examples
        if (path.matches("^/collections/[^//]+/?") && method== HttpMethods.PATCH)
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schema)
                    .schemaRef(SCHEMA_REF)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException(String.format("Unexpected path/method. Path: %s. Method: %s", path, method));
    }

    private boolean isNewStyleInfo(Path styleInfosStore, String apiId, String collectionId) {

        return Files.notExists(styleInfosStore.resolve(apiId).resolve(collectionId+".json"));
    }

    /**
     * search for the style in the store and update it, or create a new one
     * @param apiId       the id of the API
     * @param collectionId           the id of the collection, for which a styleInfos document should be updated or created
     * @param payload the new Style as a byte array
     */
    private void putStylesInfoDocument(Path styleInfosStore, String apiId, String collectionId, byte[] payload) {

        Path styleFile = styleInfosStore.resolve(apiId).resolve(collectionId+".json");
        try {
            Files.write(styleFile, payload);
        } catch (IOException e) {
            throw new RuntimeException("Could not PATCH style information: " + collectionId, e);
        }
    }
}
