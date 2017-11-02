/**
 * Copyright 2017 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;


import java.util.Map;

/**
 *
 * @author fischer
 */
public class GetFeatureHits extends GetFeatureFiltered {

    public GetFeatureHits(String namespaceUri, String featureTypeName, Map<String, String> filterValues, Map<String, String> filterPaths) {
        super(namespaceUri, featureTypeName, filterValues, filterPaths);
        this.setHits();
    }
}
