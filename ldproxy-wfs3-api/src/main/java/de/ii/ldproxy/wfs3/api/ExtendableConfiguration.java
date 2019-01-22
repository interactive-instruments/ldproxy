package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;

import java.util.Map;

public interface ExtendableConfiguration {
    @JsonMerge(value = OptBoolean.FALSE)
    Map<String, ExtensionConfiguration> getExtensions();
}
