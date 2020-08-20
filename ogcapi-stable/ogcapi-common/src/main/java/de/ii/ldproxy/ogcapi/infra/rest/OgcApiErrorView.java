package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.base.Charsets;
import io.dropwizard.views.View;

public class OgcApiErrorView extends View {

    public String title;
    public Integer status;
    public String detail;
    public String instance;

    protected OgcApiErrorView(OgcApiErrorMessage errorMessage) {
        super("exception.mustache", Charsets.UTF_8);
        this.status = errorMessage.getCode();
        this.title = errorMessage.getMessage();
        this.detail = errorMessage.getDetails();
        this.instance = errorMessage.getInstance();
    }
}
