package de.ii.ldproxy.wfs3.filtertransformer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "transformerType")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface FilterTransformerConfiguration {
}
