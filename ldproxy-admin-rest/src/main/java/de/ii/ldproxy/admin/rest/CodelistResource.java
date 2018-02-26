/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.admin.rest;

import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.CodelistStore;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.Role;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.ws.rs.*;
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

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(CodelistResource.class);
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
    public Codelist getCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id) {
        return codelistStore.getResource(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Codelist addCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ String request) {
        LOGGER.getLogger().debug("CODELIST {}", request);

        Codelist codelist = null;

        try {
            codelist = jackson.getDefaultObjectMapper().readValue(request, Codelist.class);
            codelist = codelistStore.addCodelist(codelist.getSourceUrl(), codelist.getSourceType());
        } catch (IOException e) {
            LOGGER.getLogger().debug("ERROR", e);
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