package de.ii.ldproxy.resources.app;

import com.google.common.io.ByteSource;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.resources.domain.ResourceFormatExtension;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;

@Component
@Provides
@Instantiate
public class ResourceFormatAny implements ResourceFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.WILDCARD_TYPE)
            .build();

    private final Schema schemaResource = new BinarySchema();
    public final static String SCHEMA_REF_RESOURCE = "#/components/schemas/Resource";

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/resources/{resourceId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaResource)
                    .schemaRef(SCHEMA_REF_RESOURCE)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {

        // TODO add examples
        if (path.equals("/resources/{resourceId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaResource)
                    .schemaRef(SCHEMA_REF_RESOURCE)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Response getResourceResponse(byte[] resource, String resourceId, OgcApi api, ApiRequestContext requestContext) {

        // TODO: URLConnection content-type guessing doesn't seem to work well, maybe try Apache Tika
        String contentType = URLConnection.guessContentTypeFromName(resourceId);
        if (contentType==null) {
            try {
                contentType = URLConnection.guessContentTypeFromStream(ByteSource.wrap(resource).openStream());
            } catch (IOException e) {
                // nothing we can do here, just take the default
            }
        }
        if (contentType==null || contentType.isEmpty())
            contentType = "application/octet-stream";

        return Response.ok()
                .entity(resource)
                .type(contentType)
                .header("Content-Disposition", "inline; filename=\""+resourceId+"\"")
                .build();
    }

    @Override
    public Response putResource(File resourcesStore, byte[] resource, String resourceId, OgcApi api, ApiRequestContext requestContext) {

        final String datasetId = api.getId();
        File apiDir = new File(resourcesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        File resourceFile = new File(apiDir + File.separator + resourceId);

        try {
            Files.write(resourceFile.toPath(), resource);
        } catch (IOException e) {
            throw new RuntimeException("Could not PUT resource: " + resourceId);
        }

        return Response.noContent()
                .build();
    }
}
