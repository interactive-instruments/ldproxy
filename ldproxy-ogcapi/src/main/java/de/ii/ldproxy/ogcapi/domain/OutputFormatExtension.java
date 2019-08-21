/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import javax.ws.rs.core.Response;
import java.util.List;

public interface OutputFormatExtension extends OgcApiExtension {

    OgcApiMediaType getMediaType();

    Response getConformanceResponse(List<ConformanceClass> wfs3ConformanceClasses, String serviceLabel,
                                    OgcApiMediaType ogcApiMediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                    URICustomizer uriCustomizer, String staticUrlPrefix);

    Response getDatasetResponse(Dataset dataset, OgcApiDatasetData datasetData, OgcApiMediaType mediaType,
                                List<OgcApiMediaType> alternativeMediaTypes, URICustomizer uriCustomizer,
                                String staticUrlPrefix, boolean isCollections);

    default boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return true;
    }
}
