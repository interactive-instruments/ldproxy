/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.admin.rest;

import de.ii.ldproxy.codelists.CodelistOld;
import de.ii.ldproxy.codelists.CodelistStore;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {de.ii.ldproxy.admin.rest.CodelistResource.class})
@Instantiate
@Path("/admin/codelists/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class CodelistResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistResource.class);
    @Requires
    private CodelistStore codelistStore;
    @Requires
    private Jackson jackson;


    @GET
    public List<String> getCodelists(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user*/) {
        return codelistStore.getResourceIds();
    }

    @GET
    @Path("/{id}")
    public CodelistOld getCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id) {
        return codelistStore.getResource(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public CodelistOld addCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ String request) {
        LOGGER.debug("CODELIST {}", request);

        CodelistOld codelist = null;

        try {
            codelist = jackson.getDefaultObjectMapper().readValue(request, CodelistOld.class);
            codelist = codelistStore.addCodelist(codelist.getSourceUrl(), codelist.getSourceType());
        } catch (IOException e) {
            LOGGER.debug("ERROR", e);
            throw new ParseError("Codelist could not be parsed.");
        }

        return codelist;
    }

    @DELETE
    @Path("/{id}")
    public void deleteCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id) throws IOException {
        codelistStore.deleteResource(id);
    }

}