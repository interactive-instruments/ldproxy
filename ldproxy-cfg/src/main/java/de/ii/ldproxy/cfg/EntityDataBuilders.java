/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.features.domain.FeatureProvider2;

public interface EntityDataBuilders {

  default ImmutableOgcApiDataV2.Builder api() {
    return new ImmutableOgcApiDataV2.Builder()
        .entityStorageVersion(2)
        .serviceType(OgcApiDataV2.SERVICE_TYPE);
  }

  default ImmutableFeatureProviderSqlData.Builder provider() {
    return new ImmutableFeatureProviderSqlData.Builder()
        .entityStorageVersion(2)
        .providerType(FeatureProvider2.PROVIDER_TYPE)
        .featureProviderType(FeatureProviderSql.PROVIDER_TYPE);
  }

  default ImmutableCodelistData.Builder codelist() {
    return new ImmutableCodelistData.Builder();
  }
}
