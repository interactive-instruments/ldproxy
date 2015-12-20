package de.ii.ogc.wfs.proxy;

import de.ii.xsf.logging.XSFLogger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import javax.xml.stream.XMLStreamReader;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class WfsProxyCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WfsProxyCapabilitiesAnalyzer.class);
    private WfsProxyService wfsProxy;

    public WfsProxyCapabilitiesAnalyzer(WfsProxyService wfsProxy) {
        this.wfsProxy = wfsProxy;
    }

    @Override
    public void analyzeVersion(String version) {
        wfsProxy.getWfsAdapter().setVersion(version);
    }

    @Override
    public void analyzeTitle(String title) {
        /*if (gsfs.getName().equals("default") && !title.isEmpty()) {
            gsfs.setName(title);
        }
        gsfs.setDescription(gsfs.getDescription() + title);*/
    }

    @Override
    public void analyzeCopyright(String copyright) {
        //gsfs.setCopyright(copyright);
    }

    @Override
    public void analyzeNamespaces(XMLStreamReader xml) {
        for (int i = 0; i < xml.getNamespaceCount(); i++) {
            wfsProxy.getWfsAdapter().addNamespace(xml.getNamespacePrefix(i), xml.getNamespaceURI(i));
        }
    }

    @Override
    public void analyzeFeatureType(String name) {

        if (!name.equals("gml:AbstractFeature")) {
            String uri = wfsProxy.getWfsAdapter().getNsStore().getNamespaceURI(wfsProxy.getWfsAdapter().getNsStore().extractPrefix(name));
            String localName = wfsProxy.getWfsAdapter().getNsStore().getLocalName(name);

            LOGGER.debug(FrameworkMessages.WFS_FEATURETYPE_NAME, name);

            String displayName = wfsProxy.getWfsAdapter().getNsStore().getShortNamespacePrefix(uri); //getNamespacePrefix(uri);
            if (displayName.length() > 0) {
                displayName += ":" + localName;
            } else {
                displayName = localName;
            }

            String fullName = uri + ":" + localName;

            wfsProxy.getFeatureTypes().put(fullName, new WfsProxyFeatureType(localName, uri, displayName));
        }
    }

    @Override
    public void analyzeBoundingBox(String bblower, String bbupper) {

        if (bblower != null && bbupper != null) {
            double xmin = Double.parseDouble(bblower.substring(0, bblower.indexOf(" ")));
            double ymin = Double.parseDouble(bblower.substring(bblower.indexOf(" ")));
            double xmax = Double.parseDouble(bbupper.substring(0, bbupper.indexOf(" ")));
            double ymax = Double.parseDouble(bbupper.substring(bbupper.indexOf(" ")));

            analyzeBoundingBox(xmin, ymin, xmax, ymax);
        }
    }

    @Override
    public void analyzeBoundingBox(String xmin, String ymin, String xmax, String ymax) {

        double dxmin = Double.parseDouble(xmin);
        double dymin = Double.parseDouble(ymin);
        double dxmax = Double.parseDouble(xmax);
        double dymax = Double.parseDouble(ymax);

        analyzeBoundingBox(dxmin, dymin, dxmax, dymax);
    }

    public void analyzeBoundingBox(Double xmin, Double ymin, Double xmax, Double ymax) {

        /*Envelope envelope;

        SpatialReference srIn = new SpatialReference(); // 4326 by default
        SpatialReference srOut = new SpatialReference(3857);

        Double defaultXmin = -24132518.00;
        Double defaultYmin = -19783705.00;
        Double defaultXmax = 15864227.00;
        Double defaultYmax = 20682670.00;

        // TODO: we have to fix this in Transformations ....
        if (xmin <= -180 || ymin <= -90 || xmax >= 180 || ymax >= 90) {
            envelope = new Envelope(defaultXmin, defaultYmin, defaultXmax, defaultYmax, srOut);
        } else {

            if (gsfs.getSrsTransformations().isAvailable()) {
                try {
                    envelope = gsfs.getSrsTransformations().transformOutput(new Envelope(xmin, ymin, xmax, ymax, srIn), srOut);
                } catch (CrsTransformationException ex) {
                    LOGGER.warn(FrameworkMessages.TRANSFORMATION_OF_LATLONBOUNDINGBOX_FAILED_USING_WORLD_EXTENT, xmin, ymin, xmax, ymax);
                    envelope = new Envelope(defaultXmin, defaultYmin, defaultXmax, defaultYmax, srOut);
                }
            } else {
                envelope = new Envelope(xmin, ymin, xmax, ymax, srIn);
            }
        }

        if (layer != null) {
            layer.setExtent(envelope);

            //LOGGER.debug("WFS FeatureType.BoundingBox: {} {} {} {}", envelope.getXmin(), envelope.getXmax(), envelope.getYmin(), envelope.getYmax());
        }*/
    }

    @Override
    public void analyzeDefaultSRS(String name) {

        //LOGGER.info("analyzing default SRS: {}", name);
        // TODO: workaround
        if (name.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) {
            name = "EPSG:4326";
        }

        EpsgCrs sr = new EpsgCrs(name);
        //if (!gsfs.getSrsTransformations().isAvailable() || gsfs.getSrsTransformations().isSrsSupported(sr)) {
            wfsProxy.getWfsAdapter().setDefaultCrs(sr);
        /*} else {
            LOGGER.warn(FrameworkMessages.THE_SRS_NAME_IS_NOT_SUPPORTED_BY_THIS_SERVICE, name);
            //throw new RessourceNotFound("The SRS '"+name+"' is not supported by this service!");
        }*/
    }

    @Override
    public void analyzeOtherSRS(String name) {
        EpsgCrs sr = new EpsgCrs(name);
        //if (!gsfs.getSrsTransformations().isAvailable() || gsfs.getSrsTransformations().isSrsSupported(sr)) {
            wfsProxy.getWfsAdapter().addOtherCrs(sr);
        //}
    }

    @Override
    public void analyzeDCPPOST(WFS.OPERATION op, String url) {
        try {
            wfsProxy.getWfsAdapter().addUrl(new URI(url.trim()), op, WFS.METHOD.POST);
        } catch (URISyntaxException ex) {
            java.util.logging.Logger.getLogger(WfsProxyCapabilitiesAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void analyzeDCPGET(WFS.OPERATION op, String url) {
        try {
            wfsProxy.getWfsAdapter().addUrl(new URI(url.trim()), op, WFS.METHOD.GET);
        } catch (URISyntaxException ex) {
            java.util.logging.Logger.getLogger(WfsProxyCapabilitiesAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void analyzeGMLOutputFormat(String outputformat) {
        wfsProxy.getWfsAdapter().setGmlVersionFromOutputFormat(outputformat);
    }
}
