package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.base.Charsets;
import io.dropwizard.views.View;

public class OgcApiErrorView extends View {

    public String title;
    public Integer status;
    public String detail;
    public String instance;

    protected OgcApiErrorView(String title, Integer status, String detail, String instance) {
        super("exception.mustache", Charsets.UTF_8);
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }
}
