package de.ii.ldproxy.codelists;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.core.api.exceptions.BadRequest;
import de.ii.xsf.dropwizard.api.HttpClients;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.staxmate.SMInputFactory;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CodelistStoreDefault extends AbstractGenericResourceStore<Codelist, CodelistStore> implements CodelistStore {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(CodelistStoreDefault.class);
    public static final String STORE_ID = "ldproxy-codelists";

    private HttpClient httpClient;
    private HttpClient sslHttpClient;
    private SMInputFactory staxFactory;

    public CodelistStoreDefault(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore, @Requires HttpClients httpClients) {
        super(rootConfigStore, STORE_ID, jackson.getDefaultObjectMapper(), true);

        this.staxFactory = new SMInputFactory(new InputFactoryImpl());

        this.httpClient = httpClients.getDefaultHttpClient();
        this.sslHttpClient = httpClients.getUntrustedSslHttpClient("clssl");
    }

    // TODO
    @Validate
    private void start() {
        fillCache();
    }

    @Override
    protected Codelist createEmptyResource() {
        return new Codelist();
    }

    @Override
    public Codelist addCodelist(String id) throws IOException {
        Codelist codelist = new Codelist(id, id);

        addResource(codelist);

        return codelist;
    }

    @Override
    public Codelist addCodelist(String sourceUrl, IMPORT_TYPE sourceType) throws IOException {
        Codelist codelist = new Codelist(sourceUrl, sourceType);

        if (sourceType == IMPORT_TYPE.GML_DICTIONARY) {
            HttpClient client = httpClient;
            if (sourceUrl.startsWith("https")) {
                client = sslHttpClient;
            }

            HttpResponse response = client.execute(new HttpGet(sourceUrl));

            new ImportGmlDictionary(staxFactory).parse(response.getEntity(), codelist);
        } else {
            throw new BadRequest("Import type " + sourceType + " is not supported.");
        }

        addResource(codelist);

        return codelist;
    }
}
