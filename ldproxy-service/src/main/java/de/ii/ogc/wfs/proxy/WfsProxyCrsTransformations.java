package de.ii.ogc.wfs.proxy;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class WfsProxyCrsTransformations {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WfsProxyCrsTransformations.class);

    private final CrsTransformation crsTransformation;
    private final EpsgCrs wfsDefaultCrs;
    private final EpsgCrs proxyDefaultCrs;
    private final CrsTransformer defaultTransformer;
    private boolean reverseOutputAxisOrder;
    private boolean reverseInputAxisOrder;

    public WfsProxyCrsTransformations(CrsTransformation crsTransformation, EpsgCrs wfsDefaultCrs, EpsgCrs proxyDefaultCrs) {
        this.crsTransformation = crsTransformation;
        this.wfsDefaultCrs = wfsDefaultCrs;
        this.proxyDefaultCrs = proxyDefaultCrs;
        // TODO: handle transformation not available
        if (isAvailable() && !wfsDefaultCrs.equals(proxyDefaultCrs)) {
            this.defaultTransformer = crsTransformation.getTransformer(wfsDefaultCrs, proxyDefaultCrs);
        } else {
            this.defaultTransformer = null;
        }
    }

    public boolean isAvailable() {
        return crsTransformation != null;
    }

    public CrsTransformer getDefaultTransformer() {
        return defaultTransformer;
    }

}
