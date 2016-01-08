package de.ii.ldproxy.service;

import de.ii.ogc.wfs.proxy.AbstractWfsProxyService;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class LdProxyService extends AbstractWfsProxyService {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyService.class);
    public static final String SERVICE_TYPE = "ldproxy";
    private static final String INTERFACE_SPECIFICATION = "LinkedDataService";

    public LdProxyService() {
    }

    public LdProxyService(String id, String wfsUrl) {
        super(id, SERVICE_TYPE, null, new WFSAdapter(wfsUrl.trim()));

        //this.description = "";
        //String[] path = {orgid};
        //initialize(path, module);

        // TODO
        //this.analyzeWFS();
    }

    @Override
    public String getInterfaceSpecification() {
        return INTERFACE_SPECIFICATION;
    }




}
