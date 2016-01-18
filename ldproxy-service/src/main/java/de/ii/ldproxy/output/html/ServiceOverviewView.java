package de.ii.ldproxy.output.html;

import com.google.common.collect.ImmutableList;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.views.GenericView;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * @author zahnen
 */
public class ServiceOverviewView extends DatasetView {
    public ServiceOverviewView(URI uri, Object data) {
        super("services", uri, data);
        this.title = "ldproxy Service Overview";
        this.description = "ldproxy Service Overview";
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "service", "overview").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Services", true))
                .build();
    }
}
