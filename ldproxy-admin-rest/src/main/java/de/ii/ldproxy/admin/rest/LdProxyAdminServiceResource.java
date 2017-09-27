/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.admin.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.core.api.JsonViews;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.permission.Role;
import de.ii.xsf.core.api.rest.AdminServiceResource;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import io.dropwizard.jersey.caching.CacheControl;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */

@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class LdProxyAdminServiceResource extends AdminServiceResource {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyAdminServiceResource.class);
        
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
       
    
    @GET
    //@JsonView(JsonViews.AdminView.class)
    public String getAdminService(@Auth AuthenticatedUser user) {
        String s = "";
        try {
            s = jsonMapper.writerWithType(Service.class).writeValueAsString(service);
            //LOGGER.getLogger().debug("GET SERVICE {}", s);
        } catch (JsonProcessingException e) {
            LOGGER.getLogger().error("ERROR", e);
        }
        return s;
    }


    @CacheControl(noCache = true, mustRevalidate = true)
    @Path("/config")
    @GET
    //@JsonView(JsonViews.ConfigurationView.class)
    public String getServiceConfiguration(@Auth(minRole = Role.PUBLISHER) AuthenticatedUser user, @PathParam("id") String id) {
        
        response.setHeader("Cache-Control", "no-cache");
        
        //return service;

        String s = "";
        try {
            s = jsonMapper.writeValueAsString(service);
            //LOGGER.getLogger().debug("GET SERVICE CONFIG {}", s);
        } catch (JsonProcessingException e) {
            LOGGER.getLogger().error("ERROR", e);
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
    protected void callServiceOperation(AuthenticatedUser authenticatedUser, String s, String s1) {

    }

    @Override
    protected void updateService(AuthenticatedUser authUser, String id, String request) {
        
         LdProxyService o = null;
        try {
            o = jsonMapper.readValue(request, LdProxyService.class);
        } catch (IOException ex1) {
            XtraserverFrameworkException ex = new XtraserverFrameworkException();
            ex.setCode(Response.Status.BAD_REQUEST);
            ex.setHtmlCode(Response.Status.BAD_REQUEST);
            throw ex; 
        }
                
        serviceRegistry.updateService(authUser, id, o);
        
    }
}
