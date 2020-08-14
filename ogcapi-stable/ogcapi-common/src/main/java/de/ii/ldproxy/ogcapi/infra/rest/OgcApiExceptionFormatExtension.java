package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;

public interface OgcApiExceptionFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^((/[\\w\\-]+)+)$";
    }

    Object getExceptionEntity(OgcApiErrorMessage errorMessage);

}
