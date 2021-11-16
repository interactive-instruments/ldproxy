/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.geometry_simplification;

import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.math.BigDecimal;
import java.util.Map;

import static de.ii.ldproxy.ogcapi.crs.app.QueryParameterCrsFeatures.CRS;

@Component
@Provides
@Instantiate
public class QueryParameterScaleDenominatorFeatures extends ApiExtensionCache implements OgcApiQueryParameter {

    Logger LOGGER = LoggerFactory.getLogger(QueryParameterScaleDenominatorFeatures.class);

    private final CrsTransformerFactory crsTransformerFactory;

    public QueryParameterScaleDenominatorFeatures(@Requires CrsTransformerFactory crsTransformerFactory) {
        this.crsTransformerFactory = crsTransformerFactory;
    }

    @Override
    public String getName() {
        return "scale-denominator";
    }

    @Override
    public String getDescription() {
        return "This parameter can be used to specify the target scale to be used for " +
            "simplifying the geometries in the response. The value is the denominator of the scale, " +
            "i.e., for a scale of 1:100,000, the value is 100,000.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
    }

    private final Schema schema = new NumberSchema().minimum(new BigDecimal(0)).example(100000);

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeometrySimplificationConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 datasetData) {
        if (!isExtensionEnabled(datasetData.getCollections()
                                           .get(featureTypeConfiguration.getId()), GeometrySimplificationConfiguration.class)) {
            return queryBuilder;
        }
        if (parameters.containsKey("scale-denominator")) {
            double meterPerUnit = 40075017.0d/360.0d;
            if (parameters.containsKey(CRS)) {
                try {
                    Unit<?> unit = crsTransformerFactory.getCrsUnit(EpsgCrs.fromString(parameters.get(CRS)));
                    if (unit.equals(SI.METRE)) {
                        meterPerUnit = 1.0;
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }
            try {
                // 9783.9396205 ( epsilon at WebMercatorQuad zoom level 0 ) / 559082264.028717 (scale denominator at WebMercatorQuad zoom level 0)
                double maxAllowableOffset = 0.0000175d * Double.parseDouble(parameters.get("scale-denominator")) / meterPerUnit;
                queryBuilder.maxAllowableOffset(maxAllowableOffset);
                // TODO remove
                LOGGER.debug("max-allowable-offset: {}", maxAllowableOffset);

            } catch (NumberFormatException e) {
                //ignore
            }
        }

        return queryBuilder;
    }
}
