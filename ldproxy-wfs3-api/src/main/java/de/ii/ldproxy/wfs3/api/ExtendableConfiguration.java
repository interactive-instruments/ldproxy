package de.ii.ldproxy.wfs3.api;

import java.util.Map;

public interface ExtendableConfiguration {
    Map<String, ExtensionConfiguration> getExtensions();
}
