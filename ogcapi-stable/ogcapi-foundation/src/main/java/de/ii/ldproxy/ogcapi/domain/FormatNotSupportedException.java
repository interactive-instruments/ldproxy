package de.ii.ldproxy.ogcapi.domain;

public class FormatNotSupportedException extends RuntimeException {
    public FormatNotSupportedException(String message) {
        super(message);
    }
}
