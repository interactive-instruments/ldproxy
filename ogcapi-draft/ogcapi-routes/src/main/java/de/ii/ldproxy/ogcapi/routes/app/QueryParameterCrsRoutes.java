/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterCrsRoutes extends ApiExtensionCache implements OgcApiQueryParameter {

    public static final String CRS = "crs";

    private final CrsSupport crsSupport;

    @Inject
    public QueryParameterCrsRoutes(CrsSupport crsSupport) {
        this.crsSupport = crsSupport;
    }

    @Override
    public String getId() {
        return CRS+"Routes";
    }

    @Override
    public String getName() {
        return CRS;
    }

    @Override
    public String getDescription() {
        return "The coordinate reference system of the response geometries. The default is WGS 84 longitude/latitude with optional height.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.POST &&
                definitionPath.equals("/routes"));
    }

    private ConcurrentMap<Integer, Schema> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode)) {
            List<String> crsList = crsSupport.getSupportedCrsList(apiData)
                                             .stream()
                                             .map(EpsgCrs::toUriString)
                                             .collect(ImmutableList.toImmutableList());
            String defaultCrs = apiData.getExtension(RoutingConfiguration.class).map(RoutingConfiguration::getDefaultCrs).map(crs -> crs.equals(FeaturesCoreConfiguration.DefaultCrs.CRS84) ? OgcCrs.CRS84_URI : OgcCrs.CRS84h_URI).orElse(OgcCrs.CRS84_URI);
            schemaMap.put(apiHashCode, new StringSchema()._enum(crsList)._default(defaultCrs));
        }
        return schemaMap.get(apiHashCode);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 apiData) {

        if (isEnabledForApi(apiData) && parameters.containsKey(CRS)) {
            EpsgCrs targetCrs;
            try {
                targetCrs = EpsgCrs.fromString(parameters.get(CRS));
            } catch (Throwable e) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: %s", CRS, e.getMessage()), e);
            }
            if (!crsSupport.isSupported(apiData, targetCrs)) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: the crs '%s' is not supported", CRS, targetCrs.toUriString()));
            }

            queryBuilder.crs(targetCrs);
        }

        return queryBuilder;
    }
}
