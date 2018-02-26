/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
            "SELECT ?id,?title " +
            "WHERE { GRAPH <http://data.linkeddatafactory.nl/bekendmakingen/> " +
            "{ ?id  schema:contentLocation [ schema:address [ schema:postalCode ?g ]]. FILTER regex(?g, \"";
    private static final String POSTAL_QUERY_END = "\", \"i\") . ?id  schema:headline ?title } }";
    private static final String POSTAL_EXACT_QUERY_BEGIN = "prefix schema: <http://schema.org/> " +
            "SELECT ?id,?title " +
            "WHERE { GRAPH <http://data.linkeddatafactory.nl/bekendmakingen/> " +
            "{ ?id  schema:contentLocation [ schema:address [ schema:postalCode \"";
    private static final String POSTAL_EXACT_QUERY_END = "\" ]] . ?id  schema:headline ?title } }";
    private static final String LOCALITY_QUERY_BEGIN = "prefix schema: <http://schema.org/> " +
            "SELECT ?id,?title " +
            "WHERE { GRAPH <http://data.linkeddatafactory.nl/bekendmakingen/> " +
            "{ ?id  schema:contentLocation [ schema:address [ schema:addressLocality \"";
    private static final String LOCALITY_QUERY_END = "\" ]] . ?id  schema:headline ?title } }";

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

    public Map<String, String> request(String value, QUERY type) {
        Map<String,String> ids = new LinkedHashMap<>();

        HttpResponse httpResponse = null;

        try {
            HttpGet httpGet = new HttpGet(getRequestUri(value, type));
            httpResponse = httpClient.execute(httpGet, new BasicHttpContext());

            JsonNode rootNode = jsonMapper.readTree(httpResponse.getEntity().getContent());

            Iterator<JsonNode> elements = rootNode.get("results").get("bindings").elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();

                String id = node.get("id").get("value").asText();
                String title = node.get("title").get("value").asText();
                ids.put(id, title);

                LOGGER.getLogger().debug("LINK {} {}", id, title);
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
        String query = "";
        switch (type) {
            case POSTAL_CODE:
                String number = value.substring(0, 4);
                String letters = value.substring(4);
                String regex = number + "(" + letters + ")?$";

                query = POSTAL_QUERY_BEGIN + regex + POSTAL_QUERY_END;
                break;
            case POSTAL_CODE_EXACT:
                query = POSTAL_EXACT_QUERY_BEGIN + value + POSTAL_EXACT_QUERY_END;
                break;
            case ADDRESS_LOCALITY:
            default:
                query = LOCALITY_QUERY_BEGIN + value + LOCALITY_QUERY_END;
                break;
        }
        LOGGER.getLogger().debug("SPARQL QUERY: {}", query);
        return query;
    }
}
