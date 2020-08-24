package de.ii.ldproxy.ogcapi.domain;

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
