package de.ii.ldproxy.target.html;

import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import io.dropwizard.views.ViewRenderer;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextHtml implements FeatureTransformationContext {

    public abstract FeatureCollectionView getFeatureTypeDataset();
    public abstract Codelist[] getCodelists();
    public abstract ViewRenderer getMustacheRenderer();
}
