package de.ii.ldproxy.ogcapi.infra.rest;

public class OgcApiFormatNotSupportedException extends RuntimeException {
    public OgcApiFormatNotSupportedException(String message) {
        super(message);
    }
}
