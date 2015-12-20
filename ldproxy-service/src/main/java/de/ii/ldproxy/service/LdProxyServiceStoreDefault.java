package de.ii.ldproxy.service;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xsf.configstore.api.KeyNotFoundException;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.configstore.api.rest.ResourceTransaction;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.dropwizard.api.HttpClients;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.http.client.HttpClient;
import org.codehaus.staxmate.SMInputFactory;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.joda.time.DateTime;

import java.io.IOException;

/**
 * @author zahnen
 */

// TODO: could we have a generic ServiceStore with SubStores? That would enforce id uniqueness as well.
@Component
@Provides
@Instantiate
public class LdProxyServiceStoreDefault extends AbstractGenericResourceStore<LdProxyService, LdProxyServiceStore> implements LdProxyServiceStore {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyServiceStoreDefault.class);
    public static final String SERVICE_STORE_ID = "ldproxy-services";

    // TODO: move to more generic WfsProxyServiceStore
    private HttpClient httpClient;
    private HttpClient sslHttpClient;
    private SMInputFactory staxFactory;
    private ObjectMapper jsonMapper;

    public LdProxyServiceStoreDefault(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore, @Requires HttpClients httpClients) {
        super(rootConfigStore, SERVICE_STORE_ID, jackson.getDefaultObjectMapper(), true);

        jsonMapper = jackson.getDefaultObjectMapper();

        // woodstox - more mature
        //staxFactory = new SMInputFactory(new WstxInputFactory());
        // aalto - faster
        staxFactory = new SMInputFactory(new InputFactoryImpl());

        httpClient = httpClients.getDefaultHttpClient();
        sslHttpClient = httpClients.getUntrustedSslHttpClient("wfsssl");

        // TODO: orgs layertemplates
        //this.layerTemplateStore = new WFS2GSFSLayerTemplateStore(configStores.getConfigStore(LAYER_TEMPLATES_STORE_ID), jsonMapper);
        //this.layerTemplateStores = new HashMap<String, WFS2GSFSLayerTemplateStore>();

        //this.WFS2GSFSlayerTemplateStore = new WFS2GSFSLayerTemplateStore(this.jsonMapper, layerTemplateStore);
    }

    @Override
    protected LdProxyService createEmptyResource() {
        return new LdProxyService();
    }

    // TODO: move to more generic ServiceStore
    @Override
    protected LdProxyService readResource(String[] path, String id, LdProxyService resource) throws IOException, KeyNotFoundException {
        LdProxyService service = super.readResource(path, id, resource);
        service.initialize(path, httpClient, sslHttpClient, staxFactory, jsonMapper);

        if (service.getTargetStatus() == Service.STATUS.STARTED) {
            service.start();
        }

        return service;
    }

    // TODO: move to more generic ServiceStore
    @Override
    protected void writeResource(String[] path, String resourceId, ResourceTransaction.OPERATION operation, LdProxyService resource) throws IOException {
        // caution: for update operations, "resource" contains only the changes, not the actual service
        LdProxyService service = resource;

        DateTime now = new DateTime();

        switch (operation) {
            case UPDATE:
            case UPDATE_OVERRIDE:
                resource.setLastModified(now.getMillis());
            case DELETE:
                service = getResource(path, resourceId);
                service.stop();
            case ADD:
                resource.setDateCreated(now.getMillis());

                super.writeResource(path, resourceId, operation, resource);
        }

        switch (operation) {
            case ADD:
                service.initialize(path, httpClient, sslHttpClient, staxFactory, jsonMapper);
            case UPDATE:
            case UPDATE_OVERRIDE:
                if (service.getTargetStatus() == Service.STATUS.STARTED) {
                    service.start();
                }
        }
    }

    @Override
    public LdProxyService addService(String id, String wfsUrl) throws IOException {

        //LOGGER.info(FrameworkMessages.ADDING_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL, id, queryParams.get("wfsUrl"));

        //String wfsUrl = null;
        //try {
        // TODO: what does cleanWfsUrl do? move to wfs client?
        //wfsUrl = cleanWfsUrl(queryParams.get("wfsUrl"));
        //wfsUrl = queryParams.get("wfsUrl");
        //} catch (URISyntaxException ex) {
        // TODO
        //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, queryParams.get("wfsUrl"));
        //throw new InvalidParameterValue(FrameworkMessages.FAILED_REQUESTING_URL, queryParams.get("wfsUrl"));
        //}

        LdProxyService service = new LdProxyService(id, wfsUrl);
        service.initialize(null, httpClient, sslHttpClient, staxFactory, jsonMapper);

        service.analyzeWFS();

        addResource(service);

        // TODO
        //serviceAddedPublisher.sendData(srvc);

        //LOGGER.info(FrameworkMessages.CREATED_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL, id, queryParams.get("wfsUrl"));

        return service;
    }
}
