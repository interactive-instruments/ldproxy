/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;

import de.ii.ldproxy.output.html.DatasetView;
import de.ii.xtraplatform.ogc.api.OWS;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.License;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zahnen
 */
public class GetCapabilities2OpenApi extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer {

    private OpenAPI openAPI;
    private Map<String, String> contactDetails;

    public GetCapabilities2OpenApi(OpenAPI openAPI) {
        this.openAPI = openAPI;
        openAPI.getInfo().contact(new Contact());
        this.contactDetails = new LinkedHashMap<>();
        openAPI.getInfo().getContact().addExtension("x-details", contactDetails);
    }

    @Override
    public void analyzeTitle(String title) {
        openAPI.getInfo().title(title);
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        openAPI.getInfo().description(abstrct.trim());
    }

    @Override
    public void analyzeKeywords(String... keywords) {

    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        openAPI.getInfo().license(new License().name(accessConstraints));
    }

    @Override
    public void analyzeVersion(String version) {

    }

    @Override
    public void analyzeProviderName(String providerName) {
        openAPI.getInfo().getContact().name(providerName);
    }

    @Override
    public void analyzeProviderSite(String providerSite) {

        openAPI.getInfo().getContact().url(providerSite);
    }

    @Override
    public void analyzeServiceContactIndividualName(String individualName) {

        contactDetails.put("individualName", individualName);

        if (openAPI.getInfo().getContact().getName() == null || openAPI.getInfo().getContact().getName().isEmpty() ) {
            openAPI.getInfo().getContact().name(individualName);
        }
    }

    @Override
    public void analyzeServiceContactPhone(String phone) {
        contactDetails.put("phone", phone);
    }

    @Override
    public void analyzeServiceContactFacsimile(String facsimile) {
        contactDetails.put("facsimile", facsimile);
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        openAPI.getInfo().getContact().email(email);
    }

    @Override
    public void analyzeServiceContactHoursOfService(String hoursOfService) {
        contactDetails.put("hoursOfService", hoursOfService);
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {

        contactDetails.put("onlineResource", onlineResource);

        if (openAPI.getInfo().getContact().getUrl() == null || openAPI.getInfo().getContact().getUrl().isEmpty()) {
            openAPI.getInfo().getContact().url(onlineResource);
        }
    }

    @Override
    public void analyzeServiceContactDeliveryPoint(String deliveryPoint) {
        contactDetails.put("deliveryPoint", deliveryPoint);
    }

    @Override
    public void analyzeServiceContactCity(String city) {
        contactDetails.put("city", city);
    }

    @Override
    public void analyzeServiceContactAdministrativeArea(String administrativeArea) {
        contactDetails.put("administrativeArea", administrativeArea);
    }

    @Override
    public void analyzeServiceContactPostalCode(String postalCode) {
        contactDetails.put("postalCode", postalCode);
    }

    @Override
    public void analyzeServiceContactCountry(String country) {
        contactDetails.put("country", country);
    }

    @Override
    public void analyzeInspireMetadataUrl(String metadataUrl) {

    }

    @Override
    public void analyzeFeatureTypeTitle(String featureTypeName, String title) {

    }

    @Override
    public void analyzeFeatureTypeAbstract(String featureTypeName, String abstrct) {

    }

    @Override
    public void analyzeFeatureTypeKeywords(String featureTypeName, String... keywords) {

    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {

    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {

    }

    @Override
    public void analyzeFeatureTypeMetadataUrl(String featureTypeName, String url) {

    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION operation, String url) {

    }
}
