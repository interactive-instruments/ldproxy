package de.ii.ldproxy.output.html;

import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;

/**
 * @author zahnen
 */
public class GetCapabilities2Dataset extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {
    private DatasetDAO dataset;

    public GetCapabilities2Dataset(DatasetDAO dataset) {
        this.dataset = dataset;
    }

    @Override
    public void analyzeTitle(String s) {
        // TODO : fix parser, remove aggregation
        if (dataset.name == null) {
            dataset.name = s;
        }
    }

    @Override
    public void analyzeAccessConstraints(String s) {
        dataset.license = s;
    }

    @Override
    public void analyzeVersion(String s) {

    }

    @Override
    public void analyzeFeatureType(String s) {

    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        dataset.bbox = xmin + "," + ymin + " " + xmax + "," + ymax;
    }

    @Override
    public void analyzeOperationGetUrl(WFS.OPERATION operation, String s) {
        if (operation == WFS.OPERATION.GET_CAPABILITES) {
            dataset.url = s;
        }
    }
}
