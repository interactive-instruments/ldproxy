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
    private EpsgCrs wfsDefaultCrs;
    private final EpsgCrs proxyDefaultCrs;
    private CrsTransformer defaultTransformer;
    private boolean reverseOutputAxisOrder;
    private boolean reverseInputAxisOrder;

    public WfsProxyCrsTransformations(CrsTransformation crsTransformation, EpsgCrs wfsDefaultCrs, EpsgCrs proxyDefaultCrs) {
        this.crsTransformation = crsTransformation;
        this.wfsDefaultCrs = wfsDefaultCrs;
        this.proxyDefaultCrs = proxyDefaultCrs;
        initDefaultTransformer();
    }

    private void initDefaultTransformer() {
        // TODO: handle transformation not available
        if (isAvailable() && wfsDefaultCrs != null && !wfsDefaultCrs.equals(proxyDefaultCrs)) {
            this.defaultTransformer = crsTransformation.getTransformer(wfsDefaultCrs, proxyDefaultCrs);
        }
    }

    public boolean isAvailable() {
        return crsTransformation != null;
    }

    public CrsTransformer getDefaultTransformer() {
        return defaultTransformer;
    }

    public void setWfsDefaultCrs(EpsgCrs wfsDefaultCrs) {
        this.wfsDefaultCrs = wfsDefaultCrs;
        initDefaultTransformer();
    }
}
