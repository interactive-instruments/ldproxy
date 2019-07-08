/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.generator;

import com.google.common.base.Strings;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ModifiableFeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.ModifiableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.ModifiableWfs3ServiceMetadata;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.AbstractFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.transformer.api.TemporalExtent;

/**
 * @author zahnen
 */
public class Metadata2Wfs3 extends AbstractFeatureProviderMetadataConsumer {

    private final ModifiableWfs3ServiceData wfs3ServiceData;
    private final ModifiableWfs3ServiceMetadata serviceMetadata;
    private ModifiableFeatureTypeConfigurationWfs3 currentFeatureType;

    public Metadata2Wfs3(ModifiableWfs3ServiceData wfs3ServiceData) {
        this.wfs3ServiceData = wfs3ServiceData;
        this.serviceMetadata = (ModifiableWfs3ServiceMetadata) wfs3ServiceData.getMetadata()
                                                                              .orElse(ModifiableWfs3ServiceMetadata.create());
        wfs3ServiceData.setMetadata(serviceMetadata);
    }

    @Override
    public void analyzeTitle(String title) {
        wfs3ServiceData.setLabel(title);
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        wfs3ServiceData.setDescription(abstrct);
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        serviceMetadata.addKeywords(keywords);
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        serviceMetadata.setLicenseName(accessConstraints);
    }

    @Override
    public void analyzeProviderName(String providerName) {
        serviceMetadata.setContactName(providerName);
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        if (!Strings.isNullOrEmpty(providerSite)) {
            serviceMetadata.setContactUrl(providerSite);
        }
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        serviceMetadata.setContactEmail(email);
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        if (!serviceMetadata.getContactUrl()
                            .isPresent() && !Strings.isNullOrEmpty(onlineResource)) {
            serviceMetadata.setContactUrl(onlineResource);
        }
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        currentFeatureType = ModifiableFeatureTypeConfigurationWfs3.create();
        String id = getFeatureTypeId(featureTypeName);
        currentFeatureType.setId(id);
        currentFeatureType.setLabel(getFeatureTypeName(featureTypeName));

        wfs3ServiceData.putFeatureTypes(id, currentFeatureType);
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {
        if (!Strings.isNullOrEmpty(title)) {
            currentFeatureType.setLabel(title);
        }
    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        currentFeatureType.setDescription(abstrct);
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        currentFeatureType.setExtent(new FeatureTypeConfigurationWfs3.FeatureTypeExtent(new TemporalExtent(0, 0), new BoundingBox(Double.valueOf(xmin), Double.valueOf(ymin), Double.valueOf(xmax), Double.valueOf(ymax), new EpsgCrs(4326))));
    }

    private String getFeatureTypeId(String featureTypeName) {
        return getFeatureTypeName(featureTypeName).toLowerCase();
    }

    private String getFeatureTypeName(String featureTypeName) {
        return featureTypeName.substring(featureTypeName.indexOf(":") > 0 ? featureTypeName.indexOf(":") + 1 : 0);
    }
}
