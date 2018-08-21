/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.core.api.exceptions.BadRequest;
import de.ii.xsf.dropwizard.api.HttpClients;
import de.ii.xsf.dropwizard.api.Jackson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CodelistStoreDefault extends AbstractGenericResourceStore<CodelistOld, CodelistStore> implements CodelistStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistStoreDefault.class);
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
    protected CodelistOld createEmptyResource(String id, String... paths) {
        return new CodelistOld();
    }

    @Override
    protected Class<?> getResourceClass(String id, String... path) {
        return null;
    }

    @Override
    public CodelistOld addCodelist(String id) throws IOException {
        CodelistOld codelist = new CodelistOld(id, id);

        addResource(codelist);

        return codelist;
    }

    @Override
    public CodelistOld addCodelist(String sourceUrl, IMPORT_TYPE sourceType) throws IOException {
        CodelistOld codelist = new CodelistOld(sourceUrl, sourceType);

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
