/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.geometry_simplification;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.ii.ldproxy.ogcapi.crs.app.QueryParameterCrsFeatures.CRS;

@Component
@Provides
@Instantiate
public class QueryParameterZoomLevelFeatures extends ApiExtensionCache implements OgcApiQueryParameter {

    Logger LOGGER = LoggerFactory.getLogger(QueryParameterZoomLevelFeatures.class);

    private final CrsTransformerFactory crsTransformerFactory;

    public QueryParameterZoomLevelFeatures(@Requires CrsTransformerFactory crsTransformerFactory) {
        this.crsTransformerFactory = crsTransformerFactory;
    }

    @Override
    public String getName() {
        return "zoom-level";
    }

    @Override
    public String getDescription() {
        return "This parameter can be used to specify the target scale to be used for " +
            "simplifying the geometries in the response. The value is the zoom level in " +
            "the WebMercatorQuad tiling scheme, i.e., the value 10 is for a scale of 1:545,979.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
    }

    private final Schema schema = new NumberSchema().minimum(new BigDecimal(0)).example(10);

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
                                                        Map<String, String> parameters, OgcApiDataV2 apiData) {
        if (!isExtensionEnabled(apiData.getCollections()
                                    .get(featureTypeConfiguration.getId()), GeometrySimplificationConfiguration.class)) {
            return queryBuilder;
        }
        if (parameters.containsKey("zoom-level")) {
            double meterPerUnit = getMeterPerUnit(parameters);
            try {
                // 9783.9396205 ( epsilon at WebMercatorQuad zoom level 0 ) / 559082264.028717 (scale denominator at WebMercatorQuad zoom level 0)
                double maxAllowableOffset = 9783.9396205d / Math.pow(2.0d, Double.parseDouble(parameters.get("zoom-level"))) / meterPerUnit;
                queryBuilder.maxAllowableOffset(maxAllowableOffset);
                // TODO remove
                LOGGER.debug("max-allowable-offset: {}", maxAllowableOffset);

            } catch (NumberFormatException e) {
                //ignore
            }
        }

        return queryBuilder;
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   Map<String, String> parameters, OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData, featureTypeConfiguration.getId())) {
            return parameters;
        }

        Double zoomLevel = null;
        if (parameters.containsKey("zoom-level")) {
            zoomLevel = Double.parseDouble(parameters.get("zoom-level"));
        } else if (parameters.containsKey("scale-denominator")) {
            zoomLevel = Math.max(0.0, Math.log10(559082264.029 / Double.parseDouble(parameters.get("scale-denominator"))) / 0.30102999566398114);
        } else if (parameters.containsKey("max-allowable-offset")) {
            zoomLevel = Math.max(0.0, Math.log10(9783.9396205d/Double.parseDouble(parameters.get("max-allowable-offset"))/getMeterPerUnit(parameters)) / 0.30102999566398114);
        }
        LOGGER.debug("zoom-level: {}", zoomLevel);

        if (Objects.nonNull(zoomLevel)) {
            final List<PredefinedFilter> predefFilters = apiData
                .getExtension(GeometrySimplificationConfiguration.class, featureTypeConfiguration.getId())
                .map(GeometrySimplificationConfiguration::getFilters)
                .orElse(ImmutableList.of());
            Double finalZoomLevel = zoomLevel;
            final String predefFilter = (Objects.nonNull(predefFilters)) ?
                predefFilters.stream()
                    .filter(filter -> filter.getMinZoomLevel()<=finalZoomLevel)
                    .max(Comparator.comparing(PredefinedFilter::getMinZoomLevel))
                    .flatMap(PredefinedFilter::getFilter)
                    .orElse(null) :
                null;
            if (Objects.isNull(predefFilter))
                return parameters;

            String filter = parameters.containsKey("filter")
                ? String.format("(%s) AND (%s)", parameters.get("filter"), predefFilter)
                : predefFilter;
            // TODO remove
            LOGGER.debug("filter: {}", filter);

            Map<String, String> newParameters = new HashMap<>(parameters);
            newParameters.put("filter", filter);
            return ImmutableMap.copyOf(newParameters);
        }

        return parameters;
    }

    private double getMeterPerUnit(Map<String, String> parameters) {
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
        return meterPerUnit;
    }
}
