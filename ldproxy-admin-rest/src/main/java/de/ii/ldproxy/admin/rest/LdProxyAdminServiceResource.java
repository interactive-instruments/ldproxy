/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.admin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.exceptions.BadRequest;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.rest.AdminServiceResource;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.views.ViewRenderer;
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

/**
 *
 * @author zahnen
 */

@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class LdProxyAdminServiceResource extends AdminServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdProxyAdminServiceResource.class);
        
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
       
    
    @GET
    //@JsonView(JsonViews.AdminView.class)
    public String getAdminService(/*@Auth AuthenticatedUser user*/) {
        String s = "";
        try {
            s = jsonMapper.writerWithType(Service.class).writeValueAsString(service);
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
    public String getServiceConfiguration(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user,*/ @PathParam("id") String id) {
        
        //response.setHeader("Cache-Control", "no-cache");
        
        //return service;

        String s = "";
        try {
            s = jsonMapper.writeValueAsString(service);
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
    public void init(AuthorizationProvider permProvider) {
        
    }

    @Override
    public void setMustacheRenderer(ViewRenderer mustacheRenderer) {

    }

    @Override
    protected void callServiceOperation(AuthenticatedUser authenticatedUser, String s, String s1) {

    }

    @Override
    protected void updateService(AuthenticatedUser authUser, String id, String request) {
        
         LdProxyService o = null;
        try {
            o = jsonMapper.readValue(request, LdProxyService.class);
        } catch (IOException ex1) {
            LOGGER.debug("BAD REQUEST", ex1);
            throw new BadRequest();
        }
                
        serviceRegistry.updateService(authUser, id, o);
        
    }
}
