package de.ii.ldproxy.ogcapi.domain;

import io.swagger.v3.oas.models.media.Schema;

public interface SchemaGenerator {
    Schema getSchema(Class clazz);
}
