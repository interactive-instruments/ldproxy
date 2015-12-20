package de.ii.ldproxy.rest.filter;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class QueryParamContentNegotiationFilter implements ContainerRequestFilter {

    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final Map<String, MediaType> MIME_TYPES = new ImmutableMap.Builder<String, MediaType>()
            .put("json", MediaType.APPLICATION_JSON_TYPE)
            .put("geojson", new MediaType("application","vnd.geo+json"))
            .put("html", MediaType.TEXT_HTML_TYPE)
            .put("xml", MediaType.APPLICATION_XML_TYPE)
            .build();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // Quick check for a 'f' parameter
        if (!request.getQueryParameters().containsKey(CONTENT_TYPE_PARAMETER)) {
            return request;
        }

        String format = request.getQueryParameters().getFirst(CONTENT_TYPE_PARAMETER);

        final MediaType accept = MIME_TYPES.get(format);
        if (accept != null) {
            request.getRequestHeaders().putSingle(ACCEPT_HEADER, accept.toString());
        }

        return request;
    }
}