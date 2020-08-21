package de.ii.ldproxy.ogcapi.infra.rest;

public class FormatNotSupportedException extends RuntimeException {
    public FormatNotSupportedException(String message) {
        super(message);
    }
}
