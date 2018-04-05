/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import de.ii.xtraplatform.ogc.api.OWS;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class GetCapabilities2Wfs3Dataset extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    private Map<String, Wfs3Dataset.Wfs3Collection> collections;
    private WfsCapabilities wfsCapabilities;

    public GetCapabilities2Wfs3Dataset(Wfs3Dataset wfs3Dataset) {
        this.collections = wfs3Dataset.getCollections().stream()
        .collect(Collectors.toMap(Wfs3Dataset.Wfs3Collection::getPrefixedName, collection -> collection));
        this.wfsCapabilities = wfs3Dataset.getWfsCapabilities();
    }

    @Override
    public void analyzeTitle(String title) {
        wfsCapabilities.name = title;
        wfsCapabilities.title = title;
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        wfsCapabilities.description = abstrct.trim();
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        wfsCapabilities.keywords.addAll(Arrays.asList(keywords));
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        wfsCapabilities.license = accessConstraints;
    }


    @Override
    public void analyzeProviderName(String providerName) {
        wfsCapabilities.providerName = providerName;
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        wfsCapabilities.providerUrl = providerSite;
    }

    @Override
    public void analyzeServiceContactIndividualName(String individualName) {
        wfsCapabilities.contactName = individualName;
    }

    @Override
    public void analyzeServiceContactPhone(String phone) {
        wfsCapabilities.contactTelephone = phone;
    }

    @Override
    public void analyzeServiceContactFacsimile(String facsimile) {
        wfsCapabilities.contactFaxNumber = facsimile;
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        wfsCapabilities.contactEmail = email;
    }

    @Override
    public void analyzeServiceContactHoursOfService(String hoursOfService) {
        wfsCapabilities.contactHoursOfService = hoursOfService;
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        wfsCapabilities.contactUrl = onlineResource;
    }

    @Override
    public void analyzeServiceContactDeliveryPoint(String deliveryPoint) {
        wfsCapabilities.contactStreetAddress = deliveryPoint;
    }

    @Override
    public void analyzeServiceContactCity(String city) {
        wfsCapabilities.contactLocality = city;
    }

    @Override
    public void analyzeServiceContactAdministrativeArea(String administrativeArea) {
        wfsCapabilities.contactRegion = administrativeArea;
    }

    @Override
    public void analyzeServiceContactPostalCode(String postalCode) {
        wfsCapabilities.contactPostalCode = postalCode;
    }

    @Override
    public void analyzeServiceContactCountry(String country) {
        wfsCapabilities.contactCountry = country;
    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {
        if (collections.containsKey(featureTypeName)) {
            collections.get(featureTypeName).setDescription(abstrct);
        }
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {
        wfsCapabilities.bbox = xmin + "," + ymin + " " + xmax + "," + ymax;

        if (collections.containsKey(featureTypeName)) {
            collections.get(featureTypeName).setExtent(new Wfs3Dataset.Wfs3Collection.Extent(xmin, ymin, xmax, ymax));
        }
    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION operation, String url) {
        if (operation == OWS.OPERATION.GET_CAPABILITES) {
            wfsCapabilities.url = WFS.cleanUrl(url);
        }
    }



}
