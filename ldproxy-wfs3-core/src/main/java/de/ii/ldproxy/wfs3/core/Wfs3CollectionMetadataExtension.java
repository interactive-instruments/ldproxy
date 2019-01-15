/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import de.ii.ldproxy.wfs3.api.*;

/**
 * @author zahnen
 */
public interface Wfs3CollectionMetadataExtension extends Wfs3Extension {

    ImmutableWfs3Collection.Builder process(ImmutableWfs3Collection.Builder collection, FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, URICustomizer uriCustomizer, boolean isNested, Wfs3ServiceData serviceData);
}
