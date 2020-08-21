package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;

public interface ExceptionFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^((/[\\w\\-]+)+)$";
    }

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return true;
    }

    Object getExceptionEntity(ApiErrorMessage errorMessage);

}
