package de.ii.ldproxy.ogcapi.features.html.app;

import java.util.Map.Entry;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
public interface FilterEditor {

  Set<Entry<String, String>> getFields();


}
