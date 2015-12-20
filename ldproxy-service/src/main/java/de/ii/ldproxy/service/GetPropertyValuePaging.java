package de.ii.ldproxy.service;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperationGetPropertyValue;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSQuery;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author fischer
 */
public class GetPropertyValuePaging extends WFSOperationGetPropertyValue {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WFSOperationGetPropertyValue.class);

    private int count;
    private int startIndex;
    private String nsPrefix;
    private String ftn;
    private String pn;

    public GetPropertyValuePaging(String nsPrefix, String ftn, String pn, int count, int startIndex) {
        this.nsPrefix = nsPrefix;
        this.ftn = ftn;
        this.pn = pn;
        this.count = count;
        this.startIndex = startIndex;
    }

    @Override
    protected void initialize(XMLNamespaceNormalizer nsStore) {
        
        WFSQuery wfsQuery = new WFSQuery();

        String qualifiedFeatureTypeName = nsPrefix + ":" + ftn;
        String qualifiedPropertyName = nsPrefix + ":" + pn;

        wfsQuery.addTypename(qualifiedFeatureTypeName);
        
        this.addQuery(wfsQuery);

        if (count > -1) {
            this.setCount(count);
        }
        if (startIndex > 0) {
            this.setStartIndex(startIndex);
        }

        this.setValueReference(qualifiedPropertyName);

        this.addNamespace(nsPrefix, nsStore.getNamespaceURI(nsPrefix));
    }
}
