package de.ii.ldproxy.output.html;

import de.ii.xsf.core.views.DirectoryView;

/**
 *
 * @author zahnen
 */
public class HtmlDatasetView extends DirectoryView {

    public HtmlDatasetView(Object directory, String template, String uri, String token) {
        super(directory, template, uri, token);
    }

    public String getXmlQuery() {
        return getToken().isEmpty()?"?f=xml":getToken() + "&f=xml";
    }
}
