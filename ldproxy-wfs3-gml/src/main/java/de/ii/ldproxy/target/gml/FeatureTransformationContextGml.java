package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import org.immutables.value.Value;

import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextGml implements FeatureTransformationContext {

    public abstract Map<String, String> getNamespaces();
}
