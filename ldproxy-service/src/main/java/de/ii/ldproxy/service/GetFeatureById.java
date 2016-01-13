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
