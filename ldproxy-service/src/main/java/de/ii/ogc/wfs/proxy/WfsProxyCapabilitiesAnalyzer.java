/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ogc.wfs.proxy;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author zahnen
 */
public class WfsProxyCapabilitiesAnalyzer extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer  {

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
    public void analyzeNamespace(String prefix, String uri) {
        wfsProxy.getWfsAdapter().addNamespace(prefix, uri);
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {

        if (!featureTypeName.equals("gml:AbstractFeature")) {
            String uri = wfsProxy.getWfsAdapter().getNsStore().getNamespaceURI(wfsProxy.getWfsAdapter().getNsStore().extractPrefix(featureTypeName));
            String localName = wfsProxy.getWfsAdapter().getNsStore().getLocalName(featureTypeName);

            LOGGER.debug(FrameworkMessages.WFS_FEATURETYPE_NAME, featureTypeName);

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
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {

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
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {

        //LOGGER.info("analyzing default SRS: {}", name);
        // TODO: workaround
        if (crs.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) {
            crs = "EPSG:4326";
        }

        EpsgCrs sr = new EpsgCrs(crs);
        //if (!gsfs.getSrsTransformations().isAvailable() || gsfs.getSrsTransformations().isSrsSupported(sr)) {
            wfsProxy.getWfsAdapter().setDefaultCrs(sr);
        /*} else {
            LOGGER.warn(FrameworkMessages.THE_SRS_NAME_IS_NOT_SUPPORTED_BY_THIS_SERVICE, name);
            //throw new RessourceNotFound("The SRS '"+name+"' is not supported by this service!");
        }*/
    }

    @Override
    public void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs) {
        EpsgCrs sr = new EpsgCrs(crs);
        //if (!gsfs.getSrsTransformations().isAvailable() || gsfs.getSrsTransformations().isSrsSupported(sr)) {
            wfsProxy.getWfsAdapter().addOtherCrs(sr);
        //}
    }

    @Override
    public void analyzeOperationPostUrl(WFS.OPERATION op, String url) {
        try {
            wfsProxy.getWfsAdapter().addUrl(new URI(url.trim()), op, WFS.METHOD.POST);
        } catch (URISyntaxException ex) {
            // TODO
        }
    }

    @Override
    public void analyzeOperationGetUrl(WFS.OPERATION op, String url) {
        try {
            wfsProxy.getWfsAdapter().addUrl(new URI(url.trim()), op, WFS.METHOD.GET);
        } catch (URISyntaxException ex) {
            // TODO
        }
    }

    @Override
    public void analyzeOperationParameter(WFS.OPERATION operation, WFS.VOCABULARY parameterName, String value) {
        if (parameterName == WFS.VOCABULARY.OUTPUT_FORMAT) {
            wfsProxy.getWfsAdapter().setGmlVersionFromOutputFormat(value);
        }
    }
}
