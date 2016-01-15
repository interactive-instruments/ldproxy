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
package de.ii.ldproxy.output.html;

import com.google.common.base.Strings;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;

/**
 * @author zahnen
 */
public class GetCapabilities2Dataset extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {
    private DatasetDTO dataset;

    public GetCapabilities2Dataset(DatasetDTO dataset) {
        this.dataset = dataset;
    }

    @Override
    public void analyzeTitle(String title) {
        dataset.name = title;
        dataset.title = title;
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        dataset.description = abstrct.trim();
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        for (String keyword: keywords) {
            dataset.keywords.add(keyword);
        }
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        dataset.license = accessConstraints;
    }

    @Override
    public void analyzeVersion(String version) {

    }

    @Override
    public void analyzeProviderName(String providerName) {
        dataset.providerName = providerName;
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        dataset.providerUrl = providerSite;
    }

    @Override
    public void analyzeServiceContactIndividualName(String individualName) {
        dataset.contactName = individualName;
    }

    @Override
    public void analyzeServiceContactPhone(String phone) {
        dataset.contactTelephone = phone;
    }

    @Override
    public void analyzeServiceContactFacsimile(String facsimile) {
        dataset.contactFaxNumber = facsimile;
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        dataset.contactEmail = email;
    }

    @Override
    public void analyzeServiceContactHoursOfService(String hoursOfService) {
        dataset.contactHoursOfService = hoursOfService;
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        dataset.contactUrl = onlineResource;
    }

    @Override
    public void analyzeServiceContactDeliveryPoint(String deliveryPoint) {
        dataset.contactStreetAddress = deliveryPoint;
    }

    @Override
    public void analyzeServiceContactCity(String city) {
        dataset.contactLocality = city;
    }

    @Override
    public void analyzeServiceContactAdministrativeArea(String administrativeArea) {
        dataset.contactRegion = administrativeArea;
    }

    @Override
    public void analyzeServiceContactPostalCode(String postalCode) {
        dataset.contactPostalCode = postalCode;
    }

    @Override
    public void analyzeServiceContactCountry(String country) {
        dataset.contactCountry = country;
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {

    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        for (DatasetDTO ft: dataset.featureTypes) {
            if (featureTypeName.endsWith(ft.name)) {
                ft.description = abstrct;
            }
        }
    }

    @Override
    public void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords) {
        for (DatasetDTO ft: dataset.featureTypes) {
            if (featureTypeName.endsWith(ft.name)) {
                for (String keyword: keywords) {
                    ft.keywords.add(keyword);
                }
            }
        }

    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {

    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        dataset.bbox = xmin + "," + ymin + " " + xmax + "," + ymax;

        for (DatasetDTO ft: dataset.featureTypes) {
            if (featureTypeName.endsWith(ft.name)) {
                ft.bbox = dataset.bbox;
            }
        }
    }

    @Override
    public void analyzeOperationGetUrl(WFS.OPERATION operation, String url) {
        if (operation == WFS.OPERATION.GET_CAPABILITES) {
            dataset.url = url;
        }
    }
}
