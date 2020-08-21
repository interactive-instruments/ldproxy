package de.ii.ldproxy.ogcapi.common.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.infra.rest.ApiErrorMessage;
import io.dropwizard.views.View;

public class OgcApiErrorView extends View {

    public String title;
    public Integer status;
    public String detail;
    public String instance;

    protected OgcApiErrorView(ApiErrorMessage errorMessage) {
        super("/templates/exception.mustache", Charsets.UTF_8);
        this.status = errorMessage.getCode();
        this.title = errorMessage.getMessage();
        this.detail = errorMessage.getDetails();
        this.instance = errorMessage.getInstance();
    }
}
