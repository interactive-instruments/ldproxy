/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFCommon extends QueryParameterF {

    public QueryParameterFCommon(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fCommon";
    }

    @Override
    protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.matches("^/?(?:conformance)?/?$");
    }

    @Override
    protected Class<? extends GenericFormatExtension> getFormatClass() {
        return CommonFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CommonConfiguration.class;
    }
}
