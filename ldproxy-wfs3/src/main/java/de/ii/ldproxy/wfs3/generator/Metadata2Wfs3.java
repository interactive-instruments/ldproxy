/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.generator;

import com.google.common.base.Strings;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableMetadata;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.AbstractFeatureProviderMetadataConsumer;

import java.util.Objects;

/**
 * @author zahnen
 */
public class Metadata2Wfs3 extends AbstractFeatureProviderMetadataConsumer {

    // TODO review

    private final ImmutableOgcApiDatasetData.Builder wfs3ServiceData;
    private final ImmutableMetadata.Builder serviceMetadata;
    private ImmutableFeatureTypeConfigurationOgcApi.Builder currentFeatureType;

    public Metadata2Wfs3(ImmutableOgcApiDatasetData.Builder wfs3ServiceData) {
        this.wfs3ServiceData = wfs3ServiceData;
        this.serviceMetadata = wfs3ServiceData.metadataBuilder();
    }

    @Override
    public void analyzeTitle(String title) {
        wfs3ServiceData.label(title);
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        wfs3ServiceData.description(abstrct);
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        serviceMetadata.addKeywords(keywords);
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        serviceMetadata.licenseName(accessConstraints);
    }

    @Override
    public void analyzeProviderName(String providerName) {
        serviceMetadata.contactName(providerName);
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        if (!Strings.isNullOrEmpty(providerSite)) {
            serviceMetadata.contactUrl(providerSite);
        }
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        serviceMetadata.contactEmail(email);
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        if (!serviceMetadata.build()
                            .getContactUrl()
                            .isPresent() && !Strings.isNullOrEmpty(onlineResource)) {
            serviceMetadata.contactUrl(onlineResource);
        }
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        finishLastFeatureType();

        currentFeatureType = new ImmutableFeatureTypeConfigurationOgcApi.Builder();
        String id = getFeatureTypeId(featureTypeName);
        currentFeatureType.id(id);
        currentFeatureType.label(getFeatureTypeName(featureTypeName));
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {
        if (!Strings.isNullOrEmpty(title)) {
            currentFeatureType.label(title);
        }
    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        currentFeatureType.description(abstrct);
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax,
                                              String ymax) {
        currentFeatureType.extent(new ImmutableCollectionExtent.Builder()
                .temporal(new ImmutableTemporalExtent.Builder().start(null)
                                                               .end(null)
                                                               .build())
                .spatial(new BoundingBox(Double.valueOf(xmin), Double.valueOf(ymin), Double.valueOf(xmax), Double.valueOf(ymax), new EpsgCrs(4326)))
                .build());
    }

    @Override
    public void analyzeEnd() {
        finishLastFeatureType();
    }

    private void finishLastFeatureType() {
        if (Objects.nonNull(currentFeatureType)) {
            ImmutableFeatureTypeConfigurationOgcApi featureTypeConfiguration = currentFeatureType.build();
            wfs3ServiceData.putFeatureTypes(featureTypeConfiguration.getId(), featureTypeConfiguration);
        }
    }

    private String getFeatureTypeId(String featureTypeName) {
        return getFeatureTypeName(featureTypeName).toLowerCase();
    }

    private String getFeatureTypeName(String featureTypeName) {
        return featureTypeName.substring(featureTypeName.indexOf(":") > 0 ? featureTypeName.indexOf(":") + 1 : 0);
    }
}
