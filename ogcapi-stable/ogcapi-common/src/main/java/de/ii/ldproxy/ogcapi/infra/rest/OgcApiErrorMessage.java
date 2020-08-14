package de.ii.ldproxy.ogcapi.infra.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jersey.errors.ErrorMessage;

public class OgcApiErrorMessage extends ErrorMessage {

    private String instance;

    public OgcApiErrorMessage(String title) {
        super(title);
    }

    public OgcApiErrorMessage(int code, String title) {
        super(code, title);
    }

    public OgcApiErrorMessage(int code, String title, String detail) {
        super(code, title, detail);
    }

    public OgcApiErrorMessage(int code, String title, String detail, String instance) {
        super(code, title, detail);
        this.instance = instance;
    }

    @JsonProperty("title")
    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @JsonProperty("status")
    @Override
    public Integer getCode() {
        return super.getCode();
    }

    @JsonProperty("detail")
    @Override
    public String getDetails() {
        return super.getDetails();
    }

    @JsonProperty
    public String getInstance() {
        return instance;
    }

}