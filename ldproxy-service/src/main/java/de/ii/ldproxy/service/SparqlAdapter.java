package de.ii.ldproxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xsf.dropwizard.api.HttpClients;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {SparqlAdapter.class})
@Instantiate
public class SparqlAdapter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(SparqlAdapter.class);

    private static final String SPARQL_ENDPOINT = "http://demo.linkeddatafactory.nl:8890/sparql";
    private static final String POSTAL_QUERY_BEGIN = "prefix schema: <http://schema.org/> " +
            "SELECT ?id " +
            "WHERE { GRAPH <http://data.linkeddatafactory.nl/bekendmakingen/> " +
            "{ ?id  schema:contentLocation [ schema:address [ schema:postalCode ?g ]]. FILTER regex(?g, \"";
    private static final String POSTAL_QUERY_END = "\", \"i\") } }";
    private static final String POSTAL_EXACT_QUERY_BEGIN = "prefix schema: <http://schema.org/> " +
            "SELECT ?id " +
            "WHERE { GRAPH <http://data.linkeddatafactory.nl/bekendmakingen/> " +
            "{ ?id  schema:contentLocation [ schema:address [ schema:postalCode \"";
    private static final String POSTAL_EXACT_QUERY_END = "\" ]] } }";
    private static final String LOCALITY_QUERY_BEGIN = "prefix schema: <http://schema.org/> " +
            "SELECT ?id " +
            "WHERE { GRAPH <http://data.linkeddatafactory.nl/bekendmakingen/> " +
            "{ ?id  schema:contentLocation [ schema:address [ schema:addressLocality \"";
    private static final String LOCALITY_QUERY_END = "\" ]] } }";

    public enum QUERY {
        POSTAL_CODE,
        POSTAL_CODE_EXACT,
        ADDRESS_LOCALITY
    }

    private HttpClient httpClient;
    private ObjectMapper jsonMapper;

    public SparqlAdapter(@Requires Jackson jackson, @Requires HttpClients httpClients) {
        this.jsonMapper = jackson.getDefaultObjectMapper();
        this.httpClient = httpClients.getDefaultHttpClient();
    }

    public List<String> request(String value, QUERY type) {
        List<String> ids = new ArrayList<>();

        HttpResponse httpResponse = null;

        try {
            HttpGet httpGet = new HttpGet(getRequestUri(value, type));
            httpResponse = httpClient.execute(httpGet, new BasicHttpContext());

            JsonNode rootNode = jsonMapper.readTree(httpResponse.getEntity().getContent());

            Iterator<JsonNode> elements = rootNode.get("results").get("bindings").elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();

                ids.add(node.get("id").get("value").asText());

                LOGGER.getLogger().debug("ID {}", ids.get(ids.size() - 1));
            }

        } catch (IOException | URISyntaxException e) {
            // ignore
        } finally {

            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }


        return ids;
    }

    private URI getRequestUri(String value, QUERY type) throws URISyntaxException {
        URIBuilder uri = new URIBuilder(SPARQL_ENDPOINT);
        uri.addParameter("format", "application/json");
        uri.addParameter("query", getQuery(value, type));

        return uri.build();
    }

    private String getQuery(String value, QUERY type) {
        switch (type) {
            case POSTAL_CODE:
                String number = value.substring(0, 3);
                String letters = value.substring(4);
                String regex = number + "(" + letters + ")?$";

                return POSTAL_QUERY_BEGIN + regex + POSTAL_QUERY_END;
            case POSTAL_CODE_EXACT:
                return POSTAL_EXACT_QUERY_BEGIN + value + POSTAL_EXACT_QUERY_END;
            case ADDRESS_LOCALITY:
            default:
                return LOCALITY_QUERY_BEGIN + value + LOCALITY_QUERY_END;
        }
    }
}
