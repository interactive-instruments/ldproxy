/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

import de.ii.xtraplatform.ogc.api.OWS;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;

/**
 * @author zahnen
 */
public class GetCapabilities2Dataset extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    public static final String CSW_PROXY_URL = "http://opendatacat.net/geonetwork-geo4web/doc/dataset/";

    private DatasetView dataset;

    public GetCapabilities2Dataset(DatasetView dataset) {
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
    public void analyzeInspireMetadataUrl(String metadataUrl) {
        int eq = metadataUrl.lastIndexOf('=');
        if (eq > -1) {
            String uuid = metadataUrl.substring(eq + 1);
            dataset.metadataUrl = CSW_PROXY_URL + uuid;
        }
    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {

    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        for (DatasetView ft: dataset.featureTypes) {
            if (featureTypeName.endsWith(ft.name)) {
                ft.description = abstrct;
            }
        }
    }

    @Override
    public void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords) {
        for (DatasetView ft: dataset.featureTypes) {
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

        for (DatasetView ft: dataset.featureTypes) {
            if (featureTypeName.endsWith(ft.name)) {
                ft.bbox = dataset.bbox;
            }
        }
    }

    @Override
    public void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url) {
        int eq = url.lastIndexOf('=');
        if (eq > -1) {
            String uuid = url.substring(eq + 1);

            for (DatasetView ft : dataset.featureTypes) {
                if (featureTypeName.endsWith(ft.name)) {
                    ft.metadataUrl = CSW_PROXY_URL + uuid;
                }
            }
        }
    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION operation, String url) {
        if (operation == OWS.OPERATION.GET_CAPABILITES) {
            dataset.url = WFS.cleanUrl(url);
        }
    }
}
