package de.ii.ldproxy.rest;

import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.core.api.rest.ServiceResourceFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

/**
 *
 * @author zahnen
 */
@Component
@Provides(properties= {
            @StaticServiceProperty(name=ServiceResource.SERVICE_TYPE_KEY, type="java.lang.String", value= LdProxyService.SERVICE_TYPE)
    })
@Instantiate

public class LdProxyServiceResourceFactory implements ServiceResourceFactory {

    @Override
    public Class getServiceResourceClass() {
        return LdProxyServiceResource.class;
    }
    
}
