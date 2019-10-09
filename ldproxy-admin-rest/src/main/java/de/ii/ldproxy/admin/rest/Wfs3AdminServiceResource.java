/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.admin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.features.core.api.ImmutableWfs3ServiceStatus;
import de.ii.xtraplatform.api.MediaTypeCharset;
import de.ii.xtraplatform.api.exceptions.BadRequest;
import de.ii.xtraplatform.api.permission.AuthenticatedUser;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityRegistry;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.scheduler.api.TaskStatus;
import de.ii.xtraplatform.service.api.AbstractAdminServiceResource;
import de.ii.xtraplatform.service.api.ImmutableServiceStatus;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceStatus;
import de.ii.xtraplatform.service.api.ServiceResource;
import de.ii.xtraplatform.service.api.ServiceBackgroundTasks;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = ServiceResource.SERVICE_TYPE_KEY, type = "java.lang.String", value = "WFS3")
})
@Instantiate
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class Wfs3AdminServiceResource extends AbstractAdminServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3AdminServiceResource.class);

    @Requires
    private EntityRegistry entityRegistry;
    @Requires
    private EntityRepository entityRepository;

    @Requires
    private Jackson jackson;

    @Requires
    private ServiceBackgroundTasks serviceBackgroundTasks;

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;

    @Validate
    void onStart() {
        init(jackson.getDefaultObjectMapper(), new EntityRepositoryForType(entityRepository, Service.ENTITY_TYPE), null);
    }


    @GET
    //@JsonView(JsonViews.AdminView.class)
    public String getAdminService(/*@Auth AuthenticatedUser user*/@Context ServiceData serviceData) {
        //TODO: have a TaskStatus list for each service somewhere, get TaskStatus for service if any here and set progress (maybe generic TaskQueues)

        boolean started = entityRegistry.getEntity(Service.class, serviceData.getId())
                                        .isPresent();

        ServiceStatus serviceStatus = ImmutableServiceStatus.builder()
                                                            .from(serviceData)
                                                            .status(started ? ServiceStatus.STATUS.STARTED : ServiceStatus.STATUS.STOPPED)
                                                            .build();

        Optional<TaskStatus> currentTaskForService = serviceBackgroundTasks.getCurrentTaskForService(serviceData.getId());

        boolean loading = ((OgcApiDatasetData) serviceData).getFeatureProvider()
                                                           .getMappingStatus()
                                                           .getLoading();

        String s = "";
        try {
            ImmutableWfs3ServiceStatus.Builder wfs3ServiceStatus = ImmutableWfs3ServiceStatus.builder()
                                                                                             .from(serviceStatus)
                                                                                             .status(serviceStatus.getStatus());
            if (currentTaskForService.isPresent()) {
                wfs3ServiceStatus.hasBackgroundTask(true)
                        .progress((int) Math.round(currentTaskForService.get()
                                                                        .getProgress() * 100))
                        .message(String.format("%s: %s", currentTaskForService.get()
                                                      .getLabel(), currentTaskForService.get().getStatusMessage()));
            } else if (loading) {
                wfs3ServiceStatus.hasBackgroundTask(true)
                                 .message("Initializing");
            }

            s = jsonMapper.writeValueAsString(wfs3ServiceStatus.build());
            //LOGGER.debug("GET SERVICE {}", s);
        } catch (JsonProcessingException e) {
            LOGGER.error("ERROR", e);
        }
        return s;
    }


    @CacheControl(noCache = true, mustRevalidate = true)
    @Path("/config")
    @GET
    //@JsonView(JsonViews.ConfigurationView.class)
    public String getServiceConfiguration(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id, @Context ServiceData serviceData) {

        //response.setHeader("Cache-Control", "no-cache");

        //return service;

        String s = "";
        try {
            s = jsonMapper.writerFor(OgcApiDatasetData.class)
                          .writeValueAsString(serviceData);
            //LOGGER.debug("GET SERVICE CONFIG {}", s);
        } catch (JsonProcessingException e) {
            LOGGER.error("ERROR", e);
        }
        return s;
    }
    
    /*@CacheControl(noCache = true, mustRevalidate = true)
    @Path("/{layerid}/")
    @GET
    @JsonView(JsonViews.DefaultView.class)
    public Object getAdminLayerGET(@Auth AuthenticatedUser user, @PathParam("layerid") int layerid) {
        
        response.setHeader("Cache-Control", "no-cache");
        
        return ((LdProxyService)service).findLayer(layerid);
    }
    
    @CacheControl(noCache = true, mustRevalidate = true)
    @Path("/{layerid}/mapping")
    @GET
    public Object getAdminLayerMappingGET(@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user, @PathParam("layerid") int layerid) {
                
        response.setHeader("Cache-Control", "no-cache");
        
        WFS2GSFSLayer layer = ((WFS2GSFSService)service).getFullLayers().get(layerid);
        Map<String, Object> mapping = new HashMap();
        
        mapping.put("namespaces", layer.getWfs().getNsStore().xgetShortPrefixNamespaces());
        mapping.put("mapping", layer.getFeatureTypeMapping().getElementMappings());
        mapping.put("missingGmlId", layer.isMissingGmlId());
        mapping.put("supportsResIdQuery", layer.isSupportsResIdQuery());
        mapping.put("useAsId", layer.findIdField());
        
        return mapping;
    }
    
    @Override
    public void callServiceOperation(AuthenticatedUser authUser, String operation, String parameter) {

        LOGGER.debug(DEBUG,operation+""+parameter);
        if (operation.equals("saveLayerTemplate")) {

            try {
                WFS2GSFSLayer l = ((WFS2GSFSService)service).findLayerAll(Integer.valueOf(parameter));
                l.saveAsTemplate(authUser);
            } catch (NumberFormatException e) {
                XtraserverFrameworkException ex = new XtraserverFrameworkException();
                ex.setCode(Response.Status.BAD_REQUEST);
                ex.setHtmlCode(Response.Status.BAD_REQUEST);
                ex.addDetail(SAVE_AS_TEMPLATE_FAILED_A_LAYER_WITH_ID_IS_NOT_AVAILABLE, parameter);
                throw ex; 
            }
        } else {
            XtraserverFrameworkException ex = new XtraserverFrameworkException();
            ex.setCode(Response.Status.BAD_REQUEST);
            ex.setHtmlCode(Response.Status.BAD_REQUEST);
            ex.addDetail(THE_OPERATION_IS_UNKNOWN,operation);
            throw ex; 
        }

    }*/

    @Override
    public void callServiceOperation(AuthenticatedUser authenticatedUser, String s, String s1) {

    }

    @Override
    public void updateService(AuthenticatedUser authUser, String id, String request) {

        //Wfs3ServiceData o = null;
        try {
            //o = jsonMapper.readValue(request, ModifiableWfs3ServiceData.class);
            //serviceRegistry.updateEntity(o);
            serviceRegistry.updateEntity(id, request);
        } catch (IOException ex1) {
            LOGGER.debug("BAD REQUEST", ex1);
            throw new BadRequest();
        }

        //serviceRegistry.updateService(authUser, id, o);

    }
}
