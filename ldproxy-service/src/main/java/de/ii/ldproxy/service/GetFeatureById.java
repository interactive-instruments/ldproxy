/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.filter.*;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperationGetFeature;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSQuery;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author fischer
 */
public class GetFeatureById extends WFSOperationGetFeature {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GetFeatureById.class);
    private int count;
    private int startIndex;
    private String nsPrefix;
    private String ftn;
    private String featureid;
    //private List<WFS2GSFSLayer> layer;
    
    public GetFeatureById(String nsPrefix, String ftn, String featureid) {
        //this.layer = new ArrayList<WFS2GSFSLayer>();
        //this.layer.add(layer);
        this.nsPrefix = nsPrefix;
        this.ftn = ftn;
        this.featureid = featureid;
    }
   
    @Override
    protected void initialize(XMLNamespaceNormalizer nsStore) {
        String qualifiedFeatureTypeName = nsPrefix + ":" + ftn;


        WFSQuery wfsQuery = new WFSQuery();
        OGCFilter ogcFilter = new OGCFilter();

        OGCFilterExpression expr = new OGCResourceIdExpression(featureid);
        //expr = new OGCFilterPropertyIsEqualTo( new OGCFilterValueReference("gml:@id"), new OGCFilterLiteral(featureid));

        ogcFilter.addExpression(expr);
        wfsQuery.addFilter(ogcFilter);


        //wfsQuery.setSrs(l.getWfs().getDefaultSR());
        wfsQuery.addTypename(qualifiedFeatureTypeName);
        this.addNamespace(nsPrefix, nsStore.getNamespaceURI(nsPrefix));

        this.addQuery(wfsQuery);

        this.setCount(1);

    }
}
