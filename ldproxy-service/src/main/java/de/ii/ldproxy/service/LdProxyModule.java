/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.ServiceModule;
import de.ii.xsf.core.api.exceptions.WriteError;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
// TODO: remove
// TODO: replace this and ServiceRegistry with ServiceSuperStore and ServiceSubStores
public class LdProxyModule implements ServiceModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdProxyModule.class);

    @Requires
    private LdProxyServiceStore serviceStore;



    @Override
    public Service addService(AuthenticatedUser authenticatedUser, String id, Map<String, String> queryParams, File configDirectory) throws IOException {
        // TODO: can we remove queryParams?
        if (queryParams.containsKey("url")) {

            //LOGGER.info(FrameworkMessages.ADDING_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL, id, queryParams.get("wfsUrl"));

            String wfsUrl = null;
            boolean disableMapping = Boolean.valueOf(queryParams.get("disableMapping"));
            //try {
                // TODO: what does cleanWfsUrl do? move to wfs client?
                //wfsUrl = cleanWfsUrl(queryParams.get("wfsUrl"));
                wfsUrl = queryParams.get("url");
            //} catch (URISyntaxException ex) {
                // TODO
                //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, queryParams.get("wfsUrl"));
                //throw new InvalidParameterValue(FrameworkMessages.FAILED_REQUESTING_URL, queryParams.get("wfsUrl"));
            //}

            //LdProxyService srvc = new LdProxyService(id, wfsUrl);
            // TODO
            //srvc.setDateCreated(new DateTime().getMillis());
            //srvc.setLastModified(srvc.getDateCreated());

            if (queryParams.containsKey("user") && queryParams.containsKey("password")) {
                wfsUrl = wfsUrl.replaceFirst("^(https?://)", "$1" + queryParams.get("user") + ":" + queryParams.get("password") + "@");
            }

            LdProxyService srvc = serviceStore.addService(id, wfsUrl, disableMapping);

            // TODO
            //serviceAddedPublisher.sendData(srvc);

            //LOGGER.info(FrameworkMessages.CREATED_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL, id, queryParams.get("wfsUrl"));

            return srvc;

        } else {
            // TODO
            //throw new ReqiredParameterMissing(FrameworkMessages.REQUIRED_PARAMETER_PARAMNAME_IS_MISSING_IN_REQUEST, "wfsUrl");
            return null;
        }
    }

    @Override
    public Service updateService(AuthenticatedUser authenticatedUser, String id, Service update) {
        try {
            serviceStore.updateResourceOverrides(id, (LdProxyService) update);
        } catch (IOException ex) {

            // TODO Logging
            LOGGER.error("Error updating service with id {}", id);
            LOGGER.debug("Exception: ", ex);
            throw new WriteError();
        }

        return serviceStore.getResource(id);
    }

    @Override
    public void deleteService(AuthenticatedUser authenticatedUser, Service service) {
        try {
            // TODO: cleanup

            serviceStore.deleteResource(service.getId());
        } catch (IOException ex) {
            LOGGER.error("Error deleting service with id {}", service.getId());
        }
    }

    @Override
    public Service getService(AuthenticatedUser authenticatedUser, String id) throws IOException {
        // TODO ...
        LdProxyService service = serviceStore.getResource(id);
        return service;
    }

    @Override
    public Map<String, List<Service>> getServices() throws IOException {
        Map<String, List<Service>> services = new HashMap<>();

        for (String id : serviceStore.getResourceIds()) {
            if (!services.containsKey(null)) {
                services.put(null, new ArrayList<Service>());
            }

            try {
                services.get(null).add(getService(null, id));
                LOGGER.debug("Loaded Service: {}", id);
            } catch (IOException ex) {
                LOGGER.error("Failed to load Service: {}", id);
            }
        }

        return services;
    }

    // TODO
    @Override
    public List<Service> getServiceList() throws Exception {
        //LOGGER.debug("GET SERVICE LIST 2");
        List<Service> services = new ArrayList<>();
        return services;
    }

    @Override
    public List<Service> getServiceList(AuthenticatedUser authenticatedUser) {
        return serviceStore.getResourceIds().stream()
                    .map(serviceStore::getResource)
                    .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "ldproxy";
    }

    @Override
    public String getDescription() {
        return "";//FrameworkMessages.MODULE_DESCRIPTION.get().toString(LOGGER.getLocale());;
    }
}
