package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiErrorMessage extends io.dropwizard.jersey.errors.ErrorMessage {

    private String instance;

    public ApiErrorMessage(String title) {
        super(title);
    }

    public ApiErrorMessage(int code, String title) {
        super(code, title);
    }

    public ApiErrorMessage(int code, String title, String detail) {
        super(code, title, detail);
    }

    public ApiErrorMessage(int code, String title, String detail, String instance) {
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