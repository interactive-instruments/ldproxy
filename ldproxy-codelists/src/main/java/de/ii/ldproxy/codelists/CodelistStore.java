package de.ii.ldproxy.codelists;

import de.ii.xsf.configstore.api.rest.ResourceStore;

import java.io.IOException;

/**
 * @author zahnen
 */
public interface CodelistStore extends ResourceStore<Codelist> {

    enum IMPORT_TYPE {
        GML_DICTIONARY
    }

    Codelist addCodelist(String id) throws IOException;
    Codelist addCodelist(String sourceUrl, IMPORT_TYPE sourceType) throws IOException;
}
