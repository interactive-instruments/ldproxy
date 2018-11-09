package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "extensionType")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface FeatureTypeConfigurationExtension {
}
