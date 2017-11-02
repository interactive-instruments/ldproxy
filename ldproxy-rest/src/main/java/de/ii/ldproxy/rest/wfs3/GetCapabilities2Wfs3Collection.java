package de.ii.ldproxy.rest.wfs3;

import de.ii.ldproxy.rest.wfs3.Wfs3Collections.Wfs3Collection;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;

import java.util.Map;

/**
 * @author zahnen
 */
public class GetCapabilities2Wfs3Collection extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    private Map<String, Wfs3Collection> collections;

    public GetCapabilities2Wfs3Collection(Map<String, Wfs3Collection> collections) {
        this.collections = collections;
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        /*String name = featureTypeName.contains(":") ? featureTypeName.substring(featureTypeName.lastIndexOf(":")+1) : featureTypeName;
        if (!collections.containsKey(featureTypeName)) {
            collections.put(featureTypeName, new Wfs3Collection());
        }
        collections.get(featureTypeName).setName(name.toLowerCase());
        collections.get(featureTypeName).setTitle(name);*/
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {
        /*if (title != null && !title.isEmpty()) {
            collections.get(featureTypeName).setTitle(title);
        }*/
    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        if (collections.containsKey(featureTypeName)) {
            collections.get(featureTypeName).setDescription(abstrct);
        }
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        if (collections.containsKey(featureTypeName)) {
            collections.get(featureTypeName).setExtent(new Wfs3Collection.Extent(xmin, ymin, xmax, ymax));
        }
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {

    }

    @Override
    public void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs) {

    }
}
