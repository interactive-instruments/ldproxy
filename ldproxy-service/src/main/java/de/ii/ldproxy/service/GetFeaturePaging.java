/**
 * Copyright 2018 interactive instruments GmbH
 *
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
public class GetFeaturePaging extends GetFeatureFiltered {

    public GetFeaturePaging(String namespaceUri, String featureTypeName, int count, int startIndex) {
        this(namespaceUri, featureTypeName, count, startIndex, null, null);
    }

    public GetFeaturePaging(String namespaceUri, String featureTypeName, int count, int startIndex, Map<String, String> filterValues, Map<String, String> filterPaths) {
        super(namespaceUri, featureTypeName, filterValues, filterPaths);
        this.setCount(count);
        this.setStartIndex(startIndex);
    }
}
