package de.ii.ldproxy.output.html;

import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;

import javax.xml.stream.XMLStreamReader;

/**
 * @author zahnen
 */
public class GetCapabilities2Dataset implements WFSCapabilitiesAnalyzer {
    private DatasetDAO dataset;

    public GetCapabilities2Dataset(DatasetDAO dataset) {
        this.dataset = dataset;
    }

    @Override
    public void analyzeNamespaces(XMLStreamReader xmlStreamReader) {

    }

    @Override
    public void analyzeTitle(String s) {
        // TODO : fix parser, remove aggregation
        if (dataset.name == null) {
            dataset.name = s;
        }
    }

    @Override
    public void analyzeCopyright(String s) {
        dataset.license = s;
    }

    @Override
    public void analyzeVersion(String s) {

    }

    @Override
    public void analyzeFeatureType(String s) {

    }

    @Override
    public void analyzeBoundingBox(String s, String s1) {
        String[] p1 = s.split(" ");
        String[] p2 = s1.split(" ");

        analyzeBoundingBox(p1[0], p1[1], p2[0], p2[1]);
    }

    @Override
    public void analyzeBoundingBox(String s, String s1, String s2, String s3) {
        dataset.bbox = s + "," + s1 + " " + s2 + "," + s3;
    }

    @Override
    public void analyzeDefaultSRS(String s) {

    }

    @Override
    public void analyzeOtherSRS(String s) {

    }

    @Override
    public void analyzeDCPPOST(WFS.OPERATION operation, String s) {

    }

    @Override
    public void analyzeDCPGET(WFS.OPERATION operation, String s) {
        if (operation == WFS.OPERATION.GET_CAPABILITES) {
            dataset.url = s;
        }
    }

    @Override
    public void analyzeGMLOutputFormat(String s) {

    }
}
