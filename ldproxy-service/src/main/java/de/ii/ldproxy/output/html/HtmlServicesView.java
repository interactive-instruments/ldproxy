package de.ii.ldproxy.output.html;

import com.google.common.collect.ImmutableList;
import de.ii.xsf.core.views.GenericView;

import java.net.URI;
import java.util.List;

/**
 * @author zahnen
 */
public class HtmlServicesView extends GenericView {

    public List<NavigationDTO> breadCrumbs;

    public HtmlServicesView(URI uri, Object data) {
        super("services", uri, data);
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Services", true))
                .build();
    }
}
