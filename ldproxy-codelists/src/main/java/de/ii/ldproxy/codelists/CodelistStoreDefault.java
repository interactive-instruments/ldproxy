/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.api.exceptions.BadRequest;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.kvstore.api.rest.AbstractGenericResourceStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CodelistStoreDefault extends AbstractGenericResourceStore<CodelistOld, CodelistStore> implements CodelistStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistStoreDefault.class);
    private static final String STORE_ID = "ldproxy-codelists";

    private final HttpClient httpClient;
    private final SMInputFactory staxFactory;

    public CodelistStoreDefault(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore,
                                @Requires Http http) {
        super(rootConfigStore, STORE_ID, jackson.getDefaultObjectMapper(), true);

        this.httpClient = http.getDefaultClient();
        this.staxFactory = new SMInputFactory(new InputFactoryImpl());
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
            InputStream response = httpClient.getAsInputStream(sourceUrl);

            new ImportGmlDictionary(staxFactory).parse(response, codelist);
        } else {
            throw new BadRequest("Import type " + sourceType + " is not supported.");
        }

        addResource(codelist);

        return codelist;
    }
}
