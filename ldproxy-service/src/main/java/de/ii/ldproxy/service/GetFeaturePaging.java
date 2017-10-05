/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;


import de.ii.xtraplatform.ogc.api.filter.OGCFilter;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterLiteral;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterPropertyIsEqualTo;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterValueReference;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperationGetFeature;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSQuery;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;

/**
 *
 * @author fischer
 */
public class GetFeaturePaging extends WFSOperationGetFeature {

    //private final WFS2GSFSLayer layer;
    private int count;
    private int startIndex;
    private String nsPrefix;
    private String ftn;
    private String pn;
    private String pv;

    public GetFeaturePaging(String nsPrefix, String ftn, int count, int startIndex) {
        this.nsPrefix = nsPrefix;
        this.ftn = ftn;
        this.count = count;
        this.startIndex = startIndex;
    }

    public GetFeaturePaging(String nsPrefix, String ftn, int count, int startIndex, String pn, String pv) {
        this.nsPrefix = nsPrefix;
        this.ftn = ftn;
        this.count = count;
        this.startIndex = startIndex;
        this.pn = pn;
        this.pv = pv;
    }

    @Override
    protected void initialize(XMLNamespaceNormalizer nsStore) {

        WFSQuery wfsQuery = new WFSQuery();

        String qualifiedFeatureTypeName = nsPrefix + ":" + ftn;
        
        wfsQuery.addTypename(qualifiedFeatureTypeName);

        this.addNamespace(nsPrefix, nsStore.getNamespaceURI(nsPrefix));

        if (pn != null && pv != null) {
            String qualifiedPropertyName = nsPrefix + ":" + pn;
            OGCFilter filter = new OGCFilter();
            OGCFilterValueReference valref = new OGCFilterValueReference(qualifiedPropertyName);
            OGCFilterLiteral literal = new OGCFilterLiteral(pv);

            OGCFilterPropertyIsEqualTo like = new OGCFilterPropertyIsEqualTo(valref, literal);

            filter.addExpression(like);
            wfsQuery.addFilter(filter);
        }

        this.addQuery(wfsQuery);
        this.setCount(count);
        this.setStartIndex(startIndex);
    }
}
