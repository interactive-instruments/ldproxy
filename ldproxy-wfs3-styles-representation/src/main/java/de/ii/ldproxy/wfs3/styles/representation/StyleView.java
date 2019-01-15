package de.ii.ldproxy.wfs3.styles.representation;

import de.ii.xsf.core.views.GenericView;

import java.net.URI;

public class StyleView extends GenericView {
    public String styleUrl;
    public String serviceId;

    public StyleView(String styleUrl, String serviceId) {
        super("style", null);
        this.styleUrl = styleUrl;
        this.serviceId = serviceId;
    }
}
