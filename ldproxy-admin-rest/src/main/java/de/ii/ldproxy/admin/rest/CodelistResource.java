/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.admin.rest;

import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.CodelistData;
import de.ii.ldproxy.codelists.CodelistOld;
import de.ii.ldproxy.codelists.CodelistStore;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
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
import java.util.Map;

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
    private Jackson jackson;

    private final EntityRepository entityRepository;

    CodelistResource(@Requires EntityRepository entityRepository) {
        this.entityRepository = new EntityRepositoryForType(entityRepository, Codelist.ENTITY_TYPE);
    }

    @GET
    public List<String> getCodelists(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user*/) {
        return entityRepository.getEntityIds();
    }

    @GET
    @Path("/{id}")
    public CodelistData getCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id) {
        return (CodelistData) entityRepository.getEntityData(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public CodelistData addCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ Map<String, Object> request) {
        LOGGER.debug("CODELIST {}", request);

        try {
            //TODO: codelist generator
            return  (CodelistData) entityRepository.generateEntity(request);
        }  catch (IOException e) {
            LOGGER.error("Error adding codelist", e);
            throw new BadRequestException();
        }
    }

    @DELETE
    @Path("/{id}")
    public void deleteCodelist(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id) throws IOException {
        entityRepository.deleteEntity(id);
    }

}