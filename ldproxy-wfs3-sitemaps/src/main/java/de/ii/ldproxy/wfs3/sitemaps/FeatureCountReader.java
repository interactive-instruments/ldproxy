package de.ii.ldproxy.wfs3.sitemaps;

import de.ii.xtraplatform.feature.query.api.FeatureConsumer;

import java.util.List;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public class FeatureCountReader implements FeatureConsumer {
    private OptionalLong featureCount = OptionalLong.empty();

    public OptionalLong getFeatureCount() {
        return featureCount;
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        this.featureCount = numberMatched;
    }

    @Override
    public void onEnd() throws Exception {

    }

    @Override
    public void onFeatureStart(List<String> list) throws Exception {

    }

    @Override
    public void onFeatureEnd(List<String> list) throws Exception {

    }

    @Override
    public void onPropertyStart(List<String> list, List<Integer> list1) throws Exception {

    }

    @Override
    public void onPropertyText(String s) throws Exception {

    }

    @Override
    public void onPropertyEnd(List<String> list) throws Exception {

    }
}
